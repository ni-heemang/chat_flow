-- Flow Chat Database Initialization
CREATE DATABASE IF NOT EXISTS flowchat;

-- Create user if not exists (MySQL 8.0+ syntax)
CREATE USER IF NOT EXISTS 'flowchat'@'%' IDENTIFIED BY 'flowchatpassword';
GRANT ALL PRIVILEGES ON flowchat.* TO 'flowchat'@'%';
FLUSH PRIVILEGES;

USE flowchat;

-- Tables will be created automatically by JPA/Hibernate