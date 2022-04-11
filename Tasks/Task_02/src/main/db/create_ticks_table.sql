create table TICKS
(
    SYMBOL       VARCHAR not null
        primary key,
    TICKDATETIME TIMESTAMP,
    PRICE        LONG
);

