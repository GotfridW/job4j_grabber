create table post(
id serial primary key,
name text not null,
text text not null,
link text unique not null,
created timestamp not null
);