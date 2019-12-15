# Raw Data Archiver

[![Build Status](https://travis-ci.org/vatplanner/raw-data-archiver.svg?branch=master)](https://travis-ci.org/vatplanner/raw-data-archiver)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

When raw VATSIM data files are retrieved periodically and stored for later analysis, they can quickly consume lots of memory. The archiver provides an efficient way to store such data compressed and access it at a later time.

## Current State

- very early stage of implementation
- only loading data from storage is supported so far

## Planned Features

- *receive* VATSIM data files provided by [Status Fetcher](https://github.com/vatplanner/status-fetcher) via AMQP
- *store* VATSIM data files
  - *transition* files to compressed archives
- *load* VATSIM data files (already implemented)
- *provide* VATSIM data files back to AMQP on RPC

## Storage Concept

Actual content and meta data are stored in a separate files, both using identical time stamps indicating fetch time.

For immediate access, files of the current day are stored as single files called "transitional" in the context of this application. When the day has passed, all files for one day (indicated by fetch timestamps) are then compressed ("transitioned") into an archive, stored in a directory structure indexing archives by year and month. By experimentation, `.tar.xz` has proven to be an efficient, easy-to-access archive format to compress VATSIM data files to and still remain accessible by many tools.

## Current API State

API is currently not stable and may change without notice.

## License

The implementation and accompanying files are released under [MIT license](LICENSE.md). Parsed data is subject to policies and restrictions, see the disclaimer below.
