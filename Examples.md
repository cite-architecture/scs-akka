# Examples of Scala Cite Services Requests

> Assuming the server is running at, *e.g.* `localhost:9000`

## Library-Level Requests

- <http://localhost:9000/libraryinfo>

## Texts

### See the Text Catalog

- <http://localhost:9000/texts>
- <http://localhost:9000/textcatalog>
- <http://localhost:9000/textcatalog/urn:cts:greekLit:tlg0012.tlg001:>

### Get Valid References

- <http://localhost:9000/texts/reff/urn:cts:greekLit:tlg0016.tlg001.grc:>
- <http://localhost:9000/texts/reff/urn:cts:greekLit:tlg0016.tlg001.grc:2>
- <http://localhost:9000/texts/reff/urn:cts:greekLit:tlg0016.tlg001.grc:2-3>
- <http://localhost:9000/texts/reff/urn:cts:greekLit:tlg0012.tlg001:1>

### Get Passages

- <http://localhost:9000/texts/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1>
- <http://localhost:9000/texts/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1-1.25>
- <http://localhost:9000/texts/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:2>
- <http://localhost:9000/texts/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:2-3>
- <http://localhost:9000/texts/urn:cts:greekLit:tlg0016.tlg001.grc:1.0-1.1>
- <http://localhost:9000/texts/urn:cts:greekLit:tlg0016.tlg001:1.0-1.1>

### NGrams
- <http://localhost:9000/texts/ngram?n=4>
- <http://localhost:9000/texts/ngram?n=4&t=15>
- <http://localhost:9000/texts/ngram?n=4&t=15&s=land>
- <http://localhost:9000/texts/ngram/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?n=3>
- <http://localhost:9000/texts/ngram/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?n=3&t=5>
- [http://localhost:9000/texts/ngram/urns?ng=προσέφη+πόδας+ὠκὺς](http://localhost:9000/texts/ngram/urns?ng=προσέφη+πόδας+ὠκὺς)
- [http://localhost:9000/texts/ngram/urns/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?ng=προσέφη+πόδας+ὠκὺς](http://localhost:9000/texts/ngram/urns/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?ng=προσέφη+πόδας+ὠκὺς)
- [http://localhost:9000/texts/ngram/urns/tocorpus?ng=προσέφη+πόδας+ὠκὺς](http://localhost:9000/texts/ngram/urns/tocorpus?ng=προσέφη+πόδας+ὠκὺς)
- [http://localhost:9000/texts/ngram/urns/tocorpus/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?ng=προσέφη+πόδας+ὠκὺς](http://localhost:9000/texts/ngram/urns/tocorpus/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:?ng=προσέφη+πόδας+ὠκὺς)

### String Searches
- <http://localhost:9000/texts/find?s=earth+and+water>
- <http://localhost:9000/texts/find?s=Hippias>
- <http://localhost:9000/texts/find?s=Hippias&s=Hipparchos>
- <http://localhost:9000/texts/find/urn:cts:greekLit:tlg0016.tlg001.eng:?s=wat>
- <http://localhost:9000/texts/findAll?s=ear&s=wat>
- <http://localhost:9000/texts/findAll/urn:cts:greekLit:tlg0016.tlg001.eng:?s=ear&s=wat>

### Token Searches
- <http://localhost:9000/texts/token?t=water>
- <http://localhost:9000/texts/token/urn:cts:greekLit:tlg0016.tlg001.eng:?t=water>
- <http://localhost:9000/texts/tokens?t=earth&t=water>
- <http://localhost:9000/texts/tokens/urn:cts:greekLit:tlg0016.tlg001.eng:?t=earth&t=water>
- <http://localhost:9000/texts/tokens?dist=3&t=earth&t=water>
- <http://localhost:9000/texts/tokens/urn:cts:greekLit:tlg0016.tlg001.eng:4?dist=3&t=earth&t=water>
- <http://localhost:9000/texts/tokens/urn:cts:greekLit:tlg0016.tlg001.eng:4?dist=2&t=earth&t=water> (should result in no citations)

## Collections of Objects

### Catalog

- <http://localhost:9000/collections/>
- <http://localhost:9000/collections/urn:cite2:hmt:e4.v1:>
- <http://localhost:9000/collections/reff/urn:cite2:hmt:e4.v1:> (just URNs)
- <http://localhost:9000/collections/hasobject/urn:cite2:hmt:e4.v1:1r> should return `true`
- <http://localhost:9000/collections/hasobject/urn:cite2:hmt:e4.v1:NOTOBJECT> should return `false`
- <http://localhost:9000/collections/labelmap> (returns a map of Cite2Urn -> String, the label of each citable object)

### Objects

- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:>
- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:1r>
- <http://localhost:9000/objects/prevurn/urn:cite2:hmt:e4.v1:2v>
- <http://localhost:9000/objects/nexturn/urn:cite2:hmt:e4.v1:2v>
- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:2r-3v>
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:?offset=1&limit=10>
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:?offset=11&limit=10>
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:>

Get objects from multiple collections:	

- <http://localhost:9000/collections/objects?urn=urn:cite2:cite:datamodels.v1:&urn=urn:cite2:hmt:binaryimg.v1:&urn=urn:cite2:hmt:e4.v1:>

### Finding Objects

urn-match

- <http://localhost:9000/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12r>
- <http://localhost:9000/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12r&offset=1&limit=2> (`offset` and `limit` allow paged results; this applies to all searching requests.)
- <http://localhost:9000/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:msA.2017a:12r>
- <http://localhost:9000/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:msA.2017a:12r&parameterurn=urn:cite2:hmt:textblock.2017a.folio:>

regexmatch

- <http://localhost:9000/objects/find/regexmatch?find=[0-9]{2}>
- <http://localhost:9000/objects/find/regexmatch/urn:cite2:hmt:textblock.2017a:?find=[0-9]{2}>

stringcontains

- <http://localhost:9000/objects/find/stringcontains?find=block+20>
- <http://localhost:9000/objects/find/stringcontains/urn:cite2:hmt:textblock.2017a:?find=block+20>

valueequals


- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.fakeboolean:&value=true>
- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.fakeboolean:&value=false>

- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.rv:&value=recto>
- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.rv:&value=verso>

- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:e4.v1.sequence:&value=3>

- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.image:&value=urn:cite2:hmt:vaimg.2017a:VA013RN_0014>
- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.text:&value=urn:cts:greekLit:tlg0012.tlg001:1.151-1.175>
- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.siglum:&value=msA>

numeric less-than

- <http://localhost:9000/objects/find/numeric?n1=5&op=lt>
- <http://localhost:9000/objects/find/numeric?n1=5&op=lt&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=5&op=lt>

numeric less-than-or-equal

- <http://localhost:9000/objects/find/numeric?n1=5&op=lteq>
- <http://localhost:9000/objects/find/numeric?n1=5&op=lteq&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=5&op=lteq>

numeric equals

- <http://localhost:9000/objects/find/numeric?n1=5&op=eq>
- <http://localhost:9000/objects/find/numeric?n1=5&op=eq&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=5&op=eq>

numeric greater-than

- <http://localhost:9000/objects/find/numeric?n1=5&op=gt>
- <http://localhost:9000/objects/find/numeric?n1=5&op=gt&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=5&op=gt>

numeric greater-than-or-equal

- <http://localhost:9000/objects/find/numeric?n1=5&op=gteq>
- <http://localhost:9000/objects/find/numeric?n1=5&op=gteq&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=5&op=gteq>

numeric within

- <http://localhost:9000/objects/find/numeric?n1=4&op=within&n2=6>
- <http://localhost:9000/objects/find/numeric?n1=4&op=within&n2=6&propertyurn=urn:cite2:fufolio:msChad.2017a.sequence:>
- <http://localhost:9000/objects/find/numeric/urn:cite2:fufolio:msChad.2017a:?n1=4&op=within&n2=6>

### Data Models

- <http://localhost:9000/datamodels>
- <http://localhost:9000/modelapplies?modelurn=urn:cite2:cite:datamodels.v1:binaryimg&collurn=urn:cite2:hmt:binaryimg.v1:>
- <http://localhost:9000/collectionsformodel/urn:cite2:cite:datamodels.v1:binaryimg>
- <http://localhost:9000/modelsforcollection/urn:cite2:hmt:binaryimg.v1:>

### Images

Basic Image Retrieval

- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013> (resolve to image)
- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093> (resolve to image)
- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013?resolveImage=false> (return URL to image)
- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093?resolveImage=false> (return URL to image)

Defining a width

- <http://localhost:9000/image/200/urn:cite2:hmt:vaimg.2017a:VA012RN_0013>
- <http://localhost:9000/image/200/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093>

Defining MaxWidth and MaxHeight

- <http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013>
- <http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093>

Embedding

- ![12-recto](http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013)
- ![12-recto-detail](http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093)

### Relations

CITE Relations are associations of URN to URN, with the relationship specified by a Cite2 URN.

- <http://localhost:9000/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1> Get all relations for a URN.
- <http://localhost:9000/relations/urn:cts:greekLit:tlg0012.tlg001.perseus_grc2:1.1?filter=urn:cite2:cite:dseverbs.2017a:appearsOn> Get all relations, filtered by a relation-URN.

### Commentary Data Model

If a library includes CiteRelations and implements the Commentary datamodel, comments associated with passages of text can (optionally) be attached to replies for a corpus of texts.

- <http://localhost:9000/texts/urn:cts:greekLit:tlg0012.tlg001:1.1?commentary=true>


### Documented Scholarly Editions (DSE) Data Model

The DSE Data model consists of a CITE Collection of objects, each documenting a three-way relationship between (a) a text-bearing artifact, (b) a documentary image (ideally with a region-of-interest defined), and (c) a citable passage of text.

- <http://localhost:9000/dse/recordsforsurface/urn:cite2:hmt:msA.v1:12r> Get all DSE Records associated with a Text Bearing Artifact.
- <http://localhost:9000/dse/recordsforimage/urn:cite2:hmt:vaimg.2017a:VA012RN_0013> Get all DSE Records associated with a Citable Image. 
- <http://localhost:9000/dse/recordsfortext/urn:cts:greekLit:tlg0012.tlg001.msA:1.1> Get all DSE Records associated with a passage of text.
- <http://localhost:9000/dse/recordsfortext/urn:cts:greekLit:tlg0012.tlg001.msA:1.1-1.3> Get all DSE Records associated with a passage of text expressed by a range-URN.
- <http://localhost:9000/objects/urn:cite2:hmt:msA.v1:12r?dse=true> Get and object and any DSE records associated with that object.
- <http://localhost:9000/objects/find/urnmatch?find=urn:cite2:hmt:vaimg.2017a:VA012RN_0013&dse=true&offset=0&limit=3> Find objects and get all DSE records associated with those objects (or properties of those objects). **Note**, unles `offset` and `limit` are set, this might be a *huge* list of DSE records, and might cause the request to timeout.

