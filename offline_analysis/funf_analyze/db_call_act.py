#!/usr/bin/env python
'''Merges data from a group of Funf sqlite files into one CSV file per table.
'''
import sqlite3
from optparse import OptionParser
import os.path
import time
from dbsalvage import salvage

data_table = 'data'
file_info_table = 'file_info'
tolerance = 1.75 / 2 * 60 * 1000

def merge(db_files=None, out_file=None, overwrite=True, attempt_salvage=True):
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
        out_file = 'correlated/call_act.db'
    
    if os.path.exists(out_file):
        os.remove(out_file)
    
    out_conn = sqlite3.connect(out_file)
    out_conn.row_factory = sqlite3.Row
    out_cursor = out_conn.cursor()
    
    out_cursor.execute('create table %s (id int, device text, date int, type_of_act int, type_of_call int)' % data_table)
    
    count = 0
    for db_file in db_files:
        if db_file == out_file:
            continue
        if attempt_salvage:
            try: 
                salvage(db_file)
            except (sqlite3.OperationalError,sqlite3.DatabaseError):
                print "Unable to parse file: " + db_file
                continue
        conn1 = sqlite3.connect(db_file)
        conn1.row_factory = sqlite3.Row
        cursor1 = conn1.cursor()
        conn2 = sqlite3.connect(db_file)
        conn2.row_factory = sqlite3.Row
        cursor2 = conn2.cursor()
        try: 
            cursor1.execute("select * from %s" % file_info_table)
        except (sqlite3.OperationalError,sqlite3.DatabaseError):
            print "Unable to parse file: " + db_file
            continue
        else:
            try:
                for row in cursor1:
                    id, name, device, uuid, created = row
            except IndexError:
                print "No file info exists in: " + db_file
                continue
            print "Processing %s" % db_file
            cursor1.execute("select * from %s where probe = 'act' order by datatype asc" % data_table)
            
            for row in cursor1:
                id, device, probe, date, duration, datatype, lat, lon = row
                cursor2.execute("select * from %s where probe = 'call' and date <= %d and date >= %d and datatype < 3" % (data_table, date+tolerance, date-tolerance))

                for row2 in cursor2:
                    id2, device2, probe2, date2, duration2, datatype2, lat2, lon2 = row2
                    count = count + 1
                    new_row = (count, device, date, datatype, datatype2)
                    out_conn.execute("insert into %s values (?, ?, ?, ?, ?)" % data_table, new_row)
                        
            out_conn.commit()
    out_cursor.close()

if __name__ == '__main__':
    usage = "%prog [options] [sqlite_file1.db [sqlite_file2.db...]]"
    description = "Merges many database files into one file."
    parser = OptionParser(usage="%s\n\n%s" % (usage, description))
    parser.add_option("-o", "--output", dest="file", default=None,
                      help="Filename to merge all files into.  Will overwrite a file if it already exists.", metavar="FILE")
    (options, args) = parser.parse_args()
    try:
        merge(args, options.file)
    except Exception as e:
        import sys
        sys.exit("ERROR: " + str(e))