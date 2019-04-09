Change Log
==========

## Version 0.02.00
_2019-04-09_
 * ABI/API breaking changes across codebase
 * General refactoring and documentation improvements across codebase
 * Implemented automation primitives: pairings, timers, and rules
	* splot-traits: Updated and new traits:
	   * AutomationPairingTrait
	   * AutomationRuleTrait
	   * AutomationTimerTrait
	   * ActionsTrait
	* splot-local: Implemented individual automation primitives:
	   * LocalPairing
	   * LocalRule
	   * LocalTimer
    * splot-local: Added LocalAutomationManager to facilitate in-band management
	* splot-local: Implemented [Splot Automation Expressions](doc/automation.md)
 * splot-base: Moved implicit type conversion code to its own class: TypeConverter
 * splot-base: Move various string constants to the 'Splot' pseudo class
 * splot-base: New PropertyException: BadStateForPropertyValueException
 * splot-base: FilePersistentStateManager updates:
    * Minor refactoring and warning fixes
    * When debugging, log a dump of CBOR data
    * If backing is corrupt move it instead of deleting it
 * splot-base: FunctionalEndpoint: Introduced Modifiers
 * splot-base: FunctionalEndpoint: Method arguments can now be specified using TypedKeyValue
 * splot-base: FunctionalEndpoint: Changed argument order of child listener registration
 * splot-base: Sections are now identified using an enumeration
 * splot-local: Introduced ResourceLinks and ResourceLinkManager
 * splot-local: LocalFunctionalEndpoint: Ensure listeners get initial call immediately
 * splot-local: LocalTransitioningFunctionalEndpoint: Improved update frequency clamp
 * splot-local: LocalTransitioningFunctionalEndpoint: Improved on/off/level handling
 * smcp: SmcpTechnology: Ensure the group resource is added to .well-known/core
 * smcp: SectionResource: Whitespace and error message improvements.
 * smcp: Improved reliability of CoAP UDP tests.
 * pom.xml: Now using CoapBlaster v0.02.01
 * pom.xml: Now using CborTree v0.01.01
 * pom.xml: Fixed building with Java 10

## Version 0.01.00
_2018-11-16_
 * Initial Release
