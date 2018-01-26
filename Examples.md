# Examples of Scala Cite Services Requests

> Assuming the server is running at, *e.g.* `localhost:9000`

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

### Get Texts

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
- <http://localhost:9000/texts/allTokens?t=earth&t=water>
- <http://localhost:9000/texts/allTokens/urn:cts:greekLit:tlg0016.tlg001.eng:?t=earth&t=water>

## Collections of Objects

### Catalog

- <http://localhost:9000/collections/> 
- <http://localhost:9000/collections/urn:cite2:hmt:e4.v1:> 
- <http://localhost:9000/collections/hasobject/urn:cite2:hmt:e4.v1:1r> should return `true`
- <http://localhost:9000/collections/hasobject/urn:cite2:hmt:e4.v1:NOTOBJECT> should return `false`

### Objects

- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:> 
- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:1r> 
- <http://localhost:9000/objects/urn:cite2:hmt:e4.v1:2r-3v> 
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:?offset=1&limit=10> 
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:?offset=11&limit=10> 
- <http://localhost:9000/objects/paged/urn:cite2:hmt:e4.v1:> 

### Finding Objects

urn-match

- <http://localhost:9000/objects/find/urnmatch?find=urn:cite2:hmt:msA.2017a:12r>
- <http://localhost:9000/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:msA.2017a:12r>
- <http://localhost:9000/objects/find/urnmatch/urn:cite2:hmt:textblock.2017a:?find=urn:cite2:hmt:msA.2017a:12r&parameterurn=urn:cite2:hmt:textblock.2017a.folio>

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

- <http://localhost:9000/objects/find/valueequals?propertyurn=urn:cite2:hmt:msA.v1.image:&value=urn:cite2:hmt:vaimg.v1:VA013RN_0014>
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

### Images

Basic Image Retrieval

- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013>
- <http://localhost:9000/image/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093>

Defining a width

- <http://localhost:9000/image/200/urn:cite2:hmt:vaimg.2017a:VA012RN_0013>
- <http://localhost:9000/image/200/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093>

Defining MaxWidth and MaxHeight

- <http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013>
- <http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093>

Embedding

- ![12-recto](http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013)
- ![12-recto-detail](http://localhost:9000/image/500/500/urn:cite2:hmt:vaimg.2017a:VA012RN_0013@0.04506,0.2196,0.1344,0.10093)
