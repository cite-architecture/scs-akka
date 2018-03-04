# CITE Services (Akka Microservice)

Based on [https://github.com/theiterators/akka-http-microservice](https://github.com/theiterators/akka-http-microservice) and used according to that project's MIT License. Thanks to Łukasz Sowa from [Iterators](http://www.theiterators.com).

## Version: 0.2.0

Status:  **experimental**.  No binary releases yet.

## Running

- Clone this repository
- Get [SBT](http://www.scala-sbt.org)
- At the top-level of this repository, type `sbt run`

## Building & Running a JAR

### Configure and Build

In `src/main/resources/application.conf`, specify a path to a CEX library; the default is `library=cex/library.cex`. That path will be relative to the directory from which you invoke the `.jar`. 

In SBT:

- `sbt`
- `++2.12.3`
- `assembly`

The result will be in:

> `scs-akka/target/scala-2.12/scs.jar`

### Run

Be sure your datafile is in the proper place with the proper name. By default it is `cex/library.cex` relative to the directory from which you invoke the `scs.jar`. 

Invoke it with `java -jar scs.jar`.

The service should be running at `http://localhost:9000`. 


## Testing

`sbt test` will run all tests.

By default the service uses the CEX file at `cex/library.cex`. There is another, much larger CEX file at `cex/cite-test-huge.cex`.

The CEX file is configured in the file `src/main/resources/application.conf`. 

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

- implemented `/texts/find?s=**String**` => find all passages in repository with text content matching **String**
- implemented `/texts/find/**CTS URN**?s=**String**`  => find all passages in **CTS URN** with text content matching **String**

### Token searching services

- implemented `/texts/token?t=**String**` => find all passages in repository containing token **String**
- implemented `/texts/token/**CTS URN**?t=**String**`  => find all passages in **CTS URN** containing **String**
- implemented `/texts/allTokens?t=**String**&t=**String**…` => find all passages in repository with all tokens, **String**, **String**…
- implemented `/texts/allTokens/**CTS URN**?t=**String**&t=**String**…`  => find all passages in **CTS URN** with all tokens, **String**, **String**…



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

- implemented `/collections` => return the catalog of Cite Collections in the repository
- implemented `/collections/CITE2URN` => return the CiteCollectionDef of the Collections identified by CITE2URN
- implemented `/collections/hasobject/CITE2URN`
- implemented `/objects/CITE2URN` => returns all objects identified by a CITE2URN
- implemented `/objects/paged/CITE2URN?offset=X&limit=Z` => returns a subset of object identified by CITE2URN, starting at X (1-based), and showing Z objects. If X is greater than the number, returns an empty vector; if Z is greater, returns as many as are present.
- implemented `/objects/find/urnmatch?urn=URN`
- implemented `/objects/find/urnmatch/URN1?find=URN2[&propertyurn=CITE2URN]`
- implemented `/objects/find/regexmatch?find=URN`
- implemented `/objects/find/regexmatch/URN1?find=URN2[&propertyurn=CITE2URN]`
- implemented `/objects/find/stringcontains?find=STRING`
- implemented `/objects/find/stringcontains/CITE2URN?find=STRING[&propertyurn=CITE2URN]`
- implemented `/objects/find/valueequals?propetyurn=CITE2URN&value=ANY`
- implemented `/objects/find/numeric?n1=NUMBER&op=[lt|lteq|eq|gt|gteq|within][&n2=NUMBER]`
- implemented `/objects/find/numeric/CITE2URN?n1=NUMBER&op=[lt|lteq|eq|gt|gteq|within][&n2=NUMBER][&propertyurn=CITE2URN]`

## CITE Image Services

- implemented `/image/CITE2URN` => delivers binary image data 
- implemented `/image/WIDTH/CITE2URN` => delivers binary image data scaled to WIDTH
- implemented `/image/MAXWIDTH/MAXHEIGHT/CITE2URN` => delivers binary image data scaled to fit MAXWIDTH and MAXHEIGHT

For all of these, an optional `?resolveImage=false` parameter will deliver a service-URL, rather than binary image data.

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
