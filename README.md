# CITE Services (Akka Microservice)

Based on [https://github.com/theiterators/akka-http-microservice](https://github.com/theiterators/akka-http-microservice) and used according to that project's MIT License. Thanks to Łukasz Sowa from [Iterators](http://www.theiterators.com).

## Version: 0.1.3

Status:  **experimental**.  No binary releases yet.

## Available microservices

The following services are under development.

## Text services

- implemented `/texts` => lists all distinct work-components appearing in cited text nodes
- implemented `/texts`**CTS URN** => list of citable nodes matching **CTS URN**
- implemented `/texts/reff/`**CTS URN** => (possibly empty) list of CTS URNs matching **CTS URN**
- implemented `/texts/first/`**CTS URN** => 0 or 1 citable node; if 1, the first node matching **CTS URN**
- implemented `/texts/firsturn/`**CTS URN** => CTS URN identifying the first citable node in a text
- implemented `/texts/next/`**CTS URN** => (possibly empty) Corpus of the next chunk of text
- implemented `/texts/nexturn/`**CTS URN** => Option[CtsUrn] identifying the next chunk (node or range), or None
- implemented `/texts/prev/`**CTS URN** => (possibly empty) Corpus of the previous chunk of text
- implemented `/texts/prevurn/`**CTS URN** => Option[CtsUrn] identifying the previous chunk (node or range), or None

### Text catalog services

- implemented `/textcatalog` => lists catalog entries for all cataloged texts
- implemented `/textcatalog/`**CTS URN** =>  (possibly empty) list of catalog entries matching **CTS URN**

### String searching services

To be implemented:

- `/texts/find/**String**` => find all passages in repository with text content matching **String**
- `/texts/find/`**String**/**CTS URN**/  => find all passages in **CTS URN** with text content matching **String**
- `/texts/findAll/?t=`**String**`[&t=`**String**`]...` => find all passages in repository with content matching each **token**
- `/texts/findAll/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **String**

To be implemented:

- `/texts/token/**String**` => find all passages in repository with white-space delimited token matching **String**
- `/texts/token/`**String**/**CTS URN**/  => find all passages in **CTS URN** with white-space delimited token matching **String**
- `/texts/allTokens/?t=`**tokens**`[&t=`**tokens**`]...` => find all passages in repository with content matching each **token**
- `/texts/allTokens/`**CTS URN**`?t=`**token**`[&t=`**String**`]...` => find all passages in **CTS URN** with content matching each **token**

### Ngram histograms

- implemented `/texts/ngram?n=**N**` => compute histogram of all ngrams of size `N`
- implemented `/texts/ngram?n=**N**&t=**T**` => compute histogram of all ngrams of size `N` occurring more than `T` times
- implemented `/texts/ngram/**CTS URN**?n=**N**` => compute histogram of all ngrams of size `N` within `CTS URN`
- implemented `/texts/ngram/**CTS URN**?n=**N**&t=**T**` => compute histogram of all ngrams of size `N` occurring more than `T` times within **CTS URN**
- implemented `/texts/ngram/urns?ng=**NGRAM*` => Find passages where ngram NGRAM occurs.
- implemented `/texts/ngram/urns/**CTS URN?ng=NGRAM*` => Find passages within CTS URN where ngram NGRAM occurs.

(By default, each of these ignores punctuation. To include punctuation, add `&ignorePunctuation=false` to any request.)

**N.b.** The NGRAM string must be url-encoded. This is the responsibility of the calling app.

## CITE Collection Services

- implemented `/collections/def/CITE2URN` => return the CiteCollectionDef of the Collections identified by CITE2URN
- implemented `/collections` => return the catalog of Cite Collections in the repository
- implemented `/objects/CITE2URN` => returns all objects identified by a CITE2URN

Not Implemented

- `/collection/props/CITE2URN` => returns the properties for collection CITE2URN
- `/objects/CITE2URN` => returns all objects identified by a CITE2URN
- `/collection/hasobj/CITE2URN`

## Versions

- **0.1.3** Parameterized local vs. remote CEX file.
- **0.1.2** Added next and prev functions.
- **0.1.1** Added endpints for ngrams and catalog.
- **0.1.0** First version that responds to some CITE queries, and has tested them.
- **0.0.1** Initial framework setup; modify template for Scala 2.12.

## Author & license

If you have any questions regarding this project contact:

Christopher Blackwell <christopher.blackwell@furman.edu>.

For licensing info see LICENSE file in project's root directory.
