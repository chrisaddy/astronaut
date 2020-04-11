install:
	sqlite3 -batch cards.db  "create table cards (id INTEGER PRIMARY KEY,front TEXT,back TEXT, type TEXT);"
	lein bin
