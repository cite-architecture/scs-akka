# `scs-akka`: releases

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
