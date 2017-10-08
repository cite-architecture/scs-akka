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
