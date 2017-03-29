Change Log
==========

Version 1.0.1 *(2017-03-29)*
----------------------------

 * Remove dependency on Guava. This previously implicitly available as a transitive dependency of AutoValue
   which has since been removed.


Version 1.0.0 *(2016-05-30)*
----------------------------

 * Support for AutoValue 1.2.
 * Fix: Properly support types whose properties are all named with bean-style prefixes (eg., 'get', 'is')


Version 0.1.0 *(2016-03-21)*
----------------------------

Initial release. Only guaranteed to support AutoValue 1.2-rc1.
