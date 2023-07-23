use springsecurity;
create table user(
    ID       bigint       NOT NULL PRIMARY KEY auto_increment,
    USER_NAME     varchar(50)  NOT NULL,
    PASSWORD varchar(500) NOT NULL,
    EMAIL    varchar(50)  NOT NULL,
    ACTIVE_INDICATOR   varchar(1)   NOT NULL
);

insert into user values (1, 'nisran', '12345', 'nisran@qq.com', 'A');
# Bcrypted
insert into user values (2, 'test', '$2a$04$Lw1frUDJ4PNcnjIr7YC3XOaPH3ef1d3hunQ5zt7oH6nRUIOm5goWe', 'test@qq.com', 'A');

