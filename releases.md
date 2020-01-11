# `scs-akka`: releases

**2.0.0**: Using DSE v. 6.0.4, so a breaking change. Many dependency updates.

**1.15.6**: ohco2 10.12.5 and xcite 4.0.2

**1.15.5**: Scala 2.12.8. OHCO2 10.12.3 with better `validReff` performance.

**1.15.4**: Updated CiteObj library to 7.1.3; better testing on load for duplicate URNs.

**1.15.3**: Fixed bug when getting DSEs mapped to non-leaf-node CTS URNs.

**1.15.3**: Updated xCite library to 3.6.0; better pre-processing of relations.

**1.15.2**: DSE and Commentary should work correctly when the request is a work-level URN.

**1.15.2**: Better logging info on startup.

**1.15.1**: Pre-processing of CITE Relations for CtsUrn-Range-aware matching.

**1.15.0**: Improved discovery of Relations when a cts-range is the param urn.

**1.14.0**: Now accepting range-CTS URNs for relations.

**1.13.4**: Fixed problem involving OHCO2 and DSE/Commentaries on range urns.

**1.13.3**: Improvements to CiteRelations reporting.

**1.13.1**: Ensured that CiteRelations is being URN-aware.

**1.13.0**: Added CiteRelations and Commentary Data Model.

**1.12.0**: CORS restriction lifted.

**1.11.0**: Delivering along with CiteObjects, Cite Collection search results, and CTS corpora, upon request with the `dse=true` param, DSE objects mapped to objects and citable text passages.

**1.10.1**: Also reporting "total" and "showing" numbers for searches and paged-browsing.

**1.10.0**: Added `limit` and `offset` optional parameters for paged results on collection searches.

**1.9.0**: `collections/reff/COLLECTION-URN` returns `Vector[Cite2Urn]`

**1.8.0**: Added handling of the DSE object model (see <Examples.md>).

**1.7.0**: `collections/objects?urn=URN1&urn=URN2&urn=URN3…`.

**1.6.0**: `modelsforcollection`, `collectionsformodel`, and `modelapplies`.

**1.5.0**: Added the ability to to a `valueequal` query with a `type=` param, as an alternative to specifying a property-URN.

**1.4.0**: Added `/object/prevurn/Cite2URN` and `/object/nexturn/Cite2Urn`, providing navigation of ordered collections.

**1.3.0**: Added `/object/labelmap`, returning a Map(Cite2Urn -> String), a catalog of all objects in the CollectionRepository.

**1.2.0**: Sped up changes from 1.2.0 by about 10x; sorted out kwic output.

**1.2.0**: Add option to get citable nodes for Ngrams, instead of merely URNs.

**1.1.1**: Fixed broken image requests.

**1.1.0**: Added requests for Library info and Label for a Citable Node.

**1.0.3**: Updated libraries fix bug in parsing of CEX with multiple `ctscatalog` blocks.

**1.0.2**: Updates to library depedencies.

**1.0.1**: Updates to library depedencies.

**1.0.0**: Initial release.
