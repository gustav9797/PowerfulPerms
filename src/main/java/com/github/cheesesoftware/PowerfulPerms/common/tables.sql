-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema minecraft
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema minecraft
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `minecraft` DEFAULT CHARACTER SET utf8 ;
USE `minecraft` ;

-- -----------------------------------------------------
-- Table `minecraft`.`groupparents`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`groupparents` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupid` INT(10) UNSIGNED NOT NULL,
  `parentgroupdid` INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`grouppermissions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`grouppermissions` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupid` INT(10) UNSIGNED NOT NULL,
  `permission` VARCHAR(128) NOT NULL,
  `world` VARCHAR(128) NOT NULL,
  `server` VARCHAR(128) NOT NULL,
  `expires` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`groupprefixes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`groupprefixes` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupid` INT(10) UNSIGNED NOT NULL,
  `prefix` TEXT NOT NULL,
  `server` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`groups`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`groups` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `ladder` VARCHAR(64) NOT NULL,
  `rank` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`groupsuffixes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`groupsuffixes` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupid` INT(10) UNSIGNED NOT NULL,
  `suffix` TEXT NOT NULL,
  `server` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`playergroups`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`playergroups` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `playeruuid` VARCHAR(36) NOT NULL,
  `groupid` INT(10) UNSIGNED NOT NULL,
  `server` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`playerpermissions`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`playerpermissions` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `playeruuid` VARCHAR(36) NOT NULL,
  `permission` VARCHAR(128) NOT NULL,
  `world` VARCHAR(128) NOT NULL,
  `server` VARCHAR(128) NOT NULL,
  `expires` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `minecraft`.`players`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `minecraft`.`players` (
  `uuid` VARCHAR(36) NOT NULL DEFAULT '',
  `name` VARCHAR(32) NOT NULL,
  `prefix` TEXT NOT NULL,
  `suffix` TEXT NOT NULL,
  PRIMARY KEY (`uuid`, `name`),
  UNIQUE INDEX `uuid_UNIQUE` (`uuid` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
