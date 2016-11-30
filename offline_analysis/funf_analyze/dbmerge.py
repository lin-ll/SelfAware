#!/usr/bin/env python
'''Merges data from a group of Funf sqlite files into one CSV file per table.
'''
import sqlite3
from optparse import OptionParser
import os.path
import time
from dbsalvage import salvage

file_info_table = 'file_info'
data_table = 'data'
callprobe = "edu.mit.media.funf.probe.builtin.CallLogProbe"
smsprobe = "edu.mit.media.funf.probe.builtin.SmsProbe"
activityprobe = "edu.mit.media.funf.probe.builtin.ActivityProbe"
locationprobe = "edu.mit.media.funf.probe.builtin.SimpleLocationProbe"

def merge(db_files=None, out_file=None, overwrite=False, attempt_salvage=True):
    # Check that db_files are specified and exist
    if not db_files:
        db_files = [file for file in os.listdir(os.curdir) if file.endswith(".db") and not file.startswith("merged")]
        if not db_files: 
            raise Exception("Must specify at least one db file")
    nonexistent_files = [file for file in db_files if not os.path.exists(file)]
    if nonexistent_files:
        raise Exception("The following db files do not exist: %s" % nonexistent_files)
    
    # Use default filename if it doesn't exist
    if not out_file:
        out_file = 'merged_%d.db' % int(time.time())
    
    if os.path.exists(out_file):
        if overwrite:
            os.remove(out_file)
        else:
            raise Exception("The file '%s' already exists." % out_file)
    
    out_conn = sqlite3.connect(out_file)
    out_conn.row_factory = sqlite3.Row
    out_cursor = out_conn.cursor()
    
    out_cursor.execute('create table %s (id int, name text, device text, uuid text, created text)' % file_info_table)
    out_cursor.execute('create table %s (id int, device text, probe text, date int, duration int, datatype int, latitude float, longitude float)' % data_table)
    
    count = 0
    name = ""
    for db_file in db_files:
        if db_file == out_file:
            continue
        if attempt_salvage:
            try: 
                salvage(db_file)
            except (sqlite3.OperationalError,sqlite3.DatabaseError):
                print "Unable to parse file: " + db_file
                continue
        conn = sqlite3.connect(db_file)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        try: 
            cursor.execute("select * from %s" % file_info_table)
        except (sqlite3.OperationalError,sqlite3.DatabaseError):
            print "Unable to parse file: " + db_file
            continue
        else:
            try:
                for row in cursor:
                    id, name, device, uuid, created = row
            except IndexError:
                print "No file info exists in: " + db_file
                continue
            print "Processing %s" % db_file
            cursor.execute("select * from %s" % data_table)
            for row in cursor:
                count = count + 1
                if name == "":
                    id, device, probe, date, duration, datatype, lat, lon = row
                    new_row = (count, device, probe, date, duration, datatype, lat, lon)
                    out_conn.execute("insert into %s values (?, ?, ?, ?, ?, ?, ?, ?)" % data_table, new_row)
                else:
                    id, probe, timestamp, value = row
                    value = value[1:len(value)-1]
                    parts = value.split(",")
                    date = timestamp * 1000
                    duration = 0
                    datatype = parts[len(parts)-1].split(":")[1]
                    lat = 0
                    lon = 0
                    if probe == callprobe:
                        probe = "call"
                        date = parts[1].split(":")[1]
                        duration = parts[2].split(":")[1]
                    elif probe == smsprobe:
                        probe = "sms"
                        date = parts[2].split(":")[1]
                    elif probe == activityprobe:
                        probe = "act"
                        level = parts[0].split(":")[1]
                        level = level[1:len(level)-1]
                        activities = ["none", "low", "high"]
                        datatype = activities.index(level)
                    elif probe == locationprobe:
                        probe = "loc"
                        datatype = 0
                        lat = parts[len(parts)-6].split(":")[1]
                        lon = parts[len(parts)-5].split(":")[1]
                    new_row = (count, device, probe, int(date), int(duration), int(datatype), float(lat), float(lon))
                    out_conn.execute("insert into %s values (?, ?, ?, ?, ?, ?, ?, ?)" % data_table, new_row)
            os.rename(db_file, "processed/%s" % db_file)
            out_conn.commit()
    out_cursor.execute("create temporary table backup(a,b,c,d,e,f,g,h)")
    out_cursor.execute("insert into backup select * from %s group by probe,date,duration,datatype, latitude, longitude" % data_table)
    out_cursor.execute("drop table %s" % data_table)
    out_cursor.execute("create table %s(id,device,probe,date,duration,datatype, latitude, longitude)" % data_table)
    out_cursor.execute("insert into %s select a,b,c,d,e,f,g,h from backup" % data_table)
    out_cursor.execute("drop table backup")
    out_cursor.close()

if __name__ == '__main__':
    usage = "%prog [options] [sqlite_file1.db [sqlite_file2.db...]]"
    description = "Merges many database files into one file."
    parser = OptionParser(usage="%s\n\n%s" % (usage, description))
    parser.add_option("-o", "--output", dest="file", default=None,
                      help="Filename to merge all files into.  Will not overwrite a file if it already exists.", metavar="FILE")
    (options, args) = parser.parse_args()
    try:
        merge(args, options.file)
    except Exception as e:
        import sys
        sys.exit("ERROR: " + str(e))