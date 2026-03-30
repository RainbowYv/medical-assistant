-- init.sql
CREATE TABLE IF NOT EXISTS `appointment` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `username` varchar(100) DEFAULT NULL,
    `id_card` varchar(18) DEFAULT NULL,
    `department` varchar(100) DEFAULT NULL,
    `date` varchar(50) DEFAULT NULL,
    `time` varchar(20) DEFAULT NULL,
    `doctor_name` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;