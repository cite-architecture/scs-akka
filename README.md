# CITE Services (Akka Microservice)

Based on [https://github.com/theiterators/akka-http-microservice](https://github.com/theiterators/akka-http-microservice) and used according to that project's MIT License. Thanks to Łukasz Sowa from [Iterators](http://www.theiterators.com).

## Version: 0.1.0

Status:  **experimental**.  No binary releases yet.

## Available microservices

The following services are under development.

### Text services

To implement:

- `/texts/cex_library` => based on `cex_library`, lists all distinct work-components appearing in cited text nodes
- `/texts/cex_library/`**CTS URN** => (based on `cex_library`, possibly empty) list of citable nodes matching **CTS URN**
- `/texts/cex_library/first/`**CTS URN** => based on `cex_library`, 0 or 1 citable node; if 1, the first node matching **CTS URN**
- `/texts/cex_library/reff/`**CTS URN** => based on `cex_library`, (possibly empty) list of CTS URNs matching **CTS URN**
- `/texts/cex_library/next/`**CTS URN** => based on `cex_library`, 0 or 1 citable node; if 1, the first node matching **CTS URN**
- `/texts/cex_library/prev/`**CTS URN** => based on `cex_library`, 0 or 1 citable node; if 1, the first node matching **CTS URN**

### Text catalog services

- `/textcatalog/cex_library` => lists catalog entries for all cataloged texts
- `/textcatalog/cex_library/`**CTS URN** =>  (possibly empty) list of catalog entries matching **CTS URN**

### String searching services

- `/texts/cex_library/find/`**String**` => find all passages in repository with text content matching **String**
- `/texts/cex_library/find/`**String**/**CTS URN**/  => find all passages in **CTS URN** with text content matching **String**
- `/texts/cex_library/findAll/?t=`**String**`[&t=`**String**`]...` => find all passages in repository with content matching each **token**
- `/texts/cex_library/findAll/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **String**


- `/texts/cex_library/token/`**String**` => find all passages in repository with white-space delimited token matching **String**
- `/texts/cex_library/token/`**String**/**CTS URN**/  => find all passages in **CTS URN** with white-space delimited token matching **String**
- `/texts/cex_library/allTokens/?t=`**tokens**`[&t=`**tokens**`]...` => find all passages in repository with content matching each **token**
- `/texts/cex_library/allTokens/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **token**

### Ngram histograms

- `/texts/cex_library/ngram/histogram/`**n** => compute histogram of all ngrams of size `n`
- `/texts/cex_library/ngram/histogram/`**n**/**threshhold** => compute histogram of all ngrams of size `n` occurring more than **threshold** times
- `/texts/cex_library/ngram/histogram/`**n**/**threshhold**/**CTS URN** => compute histogram of all ngrams of size `n` occurring more than **threshold** times within **CTS URN**

## Versions

- **0.1.0** Initial framework setup; modify template for Scala 2.12.

## Author & license

If you have any questions regarding this project contact:

Christopher Blackwell <christopher.blackwell@furman.edu>.

For licensing info see LICENSE file in project's root directory.
