drop table if exists Customer;

create table Customer (
    id int not null primary key,

    name varchar(255) not null default '',
    description varchar(255),
    address varchar(255),
    isActive boolean not null default 1,

    originalId int
);


drop table if exists Project;
create table Project (
    id int not null primary key,

    name varchar(255) not null default '',
    customerId int, -- allow project without customer
    defaultRate decimal(18, 4) not null default 0,
    createdDateUTC date not null,
    isActive boolean not null default 1,

    originalId int ,
    originalCustomerId int

    -- foreign key (customerId) references Customer (id)
);


drop table if exists Settings;
create table Settings (
    id int not null primary key,

    name varchar(255) not null ,
    val varchar(255)
);

insert into Settings (id, name, val) values
(1, 'PAYMENT_RECIPIENT_TITLE', 'Company Intl. Inc');

insert into Settings (id, name, val) values
(2, 'PAYMENT_TERMS', 'On receipt');

insert into Settings (id, name, val) values
(3, 'PAYMENT_RECIPIENT_ADDRESS', 'Company Inc.
Attn: ABCD - J. Doe
1234 Long Street
City, ST 12345');
