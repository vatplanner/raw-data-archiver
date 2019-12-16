# Raw Data Archiver

[![Build Status](https://travis-ci.org/vatplanner/raw-data-archiver.svg?branch=master)](https://travis-ci.org/vatplanner/raw-data-archiver)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

When raw VATSIM data files are retrieved periodically and stored for later analysis, they can quickly consume lots of memory. The archiver provides an efficient way to store such data compressed and access it at a later time.

## Current State

- early stage of implementation
- only loading and serving data from storage is supported so far

## Planned Features

- *receive* VATSIM data files provided by [Status Fetcher](https://github.com/vatplanner/status-fetcher) via AMQP
- *store* VATSIM data files
  - *transition* files to compressed archives
- *load* VATSIM data files (implemented)
- *provide* VATSIM data files back to AMQP on RPC (implemented)

## Storage Concept

Actual content and meta data are stored in a separate files, both using identical time stamps indicating fetch time.

For immediate access, files of the current day are stored as single files called "transitional" in the context of this application. When the day has passed, all files for one day (indicated by fetch timestamps) are then compressed ("transitioned") into an archive, stored in a directory structure indexing archives by year and month. By experimentation, `.tar.xz` has proven to be an efficient, easy-to-access archive format to compress VATSIM data files to and still remain accessible by many tools.

## Current API State

API is currently not stable and may change without notice.

## RPC over RabbitMQ/AMQP

### Retrieve Data Files

#### Request

RPC requests are encoded in JSON, defining the time period to be retrieved (inclusive), the requested packer method and an optional file limit.

Example:

```json
{
  "packerMethod": "zip/deflate",
  "earliestFetchTime": "2019-12-01T00:00:00Z",
  "latestFetchTime": "2019-12-01T23:59:59Z",
  "fileLimit": 100
}
```

Available packer methods:

| `packerMethod`     | Container | Compression | Size  | Time            | Recommended Use                                                    |
| ------------------ | --------- | ----------- | ----- | --------------- | ------------------------------------------------------------------ |
| `tar`              | TAR       | none        | >100% | instant         | when RPC can be served locally or bandwidth is no concern          |
| `tar+deflate`      | TAR       | deflate     | 43%   | few seconds     | when reduction in size is sufficient, continuous stream            |
| `tar+bzip2`        | TAR       | BZIP2       | 15%   | 3x deflate/gzip | when higher reduction in size is needed and response can wait      |
| `tar+gzip`         | TAR       | GZIP        | 43%   | few seconds     | when reduction in size is sufficient, continuous stream            |
| `tar+lzma`         | TAR       | LZMA        | 4%    | 8x deflate/gzip | only if bandwidth is of high concern; response will take very long |
| `tar+xz`           | TAR       | XZ          | 4%    | 8x deflate/gzip | only if bandwidth is of high concern; response will take very long |
| `zip/deflate`      | ZIP       | deflate     | 44%   | few seconds     | when reduction in size is sufficient, needs full cache to read     |
| `zip/uncompressed` | ZIP       | none        | >100% | instant         | when RPC can be served locally or bandwidth is no concern          |

If server permits multi-threading, `zip/deflate` will be faster than `tar+deflate` or `tar+gzip`. ZIP files need random access to unpack while TAR can be unpacked as one continuous stream. Choosing the best packer method depends on the individual situation.

#### Response

Response will be sent back to the reply queue as a binary packed file using the requested compression method and also indicated by message header.

Please note that

 - requests may time out; no reponse will arrive in that case
 - responses may not cover complete requested period, limited by a server-side maximum file limit 
 - requests may get lost due to crashes, for example if OOM occurs during packing (server should reduce maximum file limit in that case)
 - slow packer methods (such as `tar+xz` or `tar+lzma`) may not only slow down a single message but also defer processing of other queued requests up to their timeout

## License

The implementation and accompanying files are released under [MIT license](LICENSE.md). Parsed data is subject to policies and restrictions set by VATSIM and your local regulations.
