# Job4j. Grabber - the Java jobs aggregator project

## Description

This project is a program for reading jobs from a web resource and recording them in the database.

The system starts on schedule - once a minute.  The start period is specified in the settings - app.properties.
The program has to read all the jobs from the first 5 pages related to Java and write them into the database.
The first site will be [Habr Career](https://career.habr.com/). 
