# CITE Services (Akka Microservice)

Based on [https://github.com/theiterators/akka-http-microservice](https://github.com/theiterators/akka-http-microservice) and used according to that project's MIT License. Thanks to Łukasz Sowa from [Iterators](http://www.theiterators.com).

## Version: 0.1.0

Status:  **experimental**.  No binary releases yet.

## Available microservices

The following services are under development.

### Text services

To implement:

- tbd `/texts` => lists all distinct work-components appearing in cited text nodes
- implemented `/texts`**CTS URN** => list of citable nodes matching **CTS URN**
- implemented `/texts/first/`**CTS URN** => 0 or 1 citable node; if 1, the first node matching **CTS URN**
- tbd `/texts/reff/`**CTS URN** => (possibly empty) list of CTS URNs matching **CTS URN**
- tbd `/texts/next/`**CTS URN** => 0 or 1 citable node; if 1, the first node matching **CTS URN**
- tbd `/texts/prev/`**CTS URN** => 0 or 1 citable node; if 1, the first node matching **CTS URN**

### Text catalog services

- tbd `/textcatalog` => lists catalog entries for all cataloged texts
- tbd `/textcatalog/`**CTS URN** =>  (possibly empty) list of catalog entries matching **CTS URN**

### String searching services

- tbd `/texts/find/`**String**` => find all passages in repository with text content matching **String**
- tbd `/texts/find/`**String**/**CTS URN**/  => find all passages in **CTS URN** with text content matching **String**
- tbd `/texts/findAll/?t=`**String**`[&t=`**String**`]...` => find all passages in repository with content matching each **token**
- tbd `/texts/findAll/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **String**


- tbd `/texts/token/`**String**` => find all passages in repository with white-space delimited token matching **String**
- tbd `/texts/token/`**String**/**CTS URN**/  => find all passages in **CTS URN** with white-space delimited token matching **String**
- tbd `/texts/allTokens/?t=`**tokens**`[&t=`**tokens**`]...` => find all passages in repository with content matching each **token**
- tbd `/texts/allTokens/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **token**

### Ngram histograms

- tbd `/texts/ngram?n=**N**` => compute histogram of all ngrams of size `N`
- tbd `/texts/ngram?n=**N**&t=**T**` => compute histogram of all ngrams of size `N` occurring more than `T` times
- tbd `/texts/ngram/**CTS URN**?n=**N**` => compute histogram of all ngrams of size `N` within `CTS URN`
- tbd `/texts/ngram/**CTS URN**?n=**N**&t=**T**` => compute histogram of all ngrams of size `N` occurring more than `T` times within **CTS URN**

(By default, each of these ignores punctuation. To include punctuation, add `&ignorePunctuation=false` to any request.)

## Versions

- **0.1.0** Initial framework setup; modify template for Scala 2.12.

## Author & license

If you have any questions regarding this project contact:

Christopher Blackwell <christopher.blackwell@furman.edu>.

For licensing info see LICENSE file in project's root directory.
