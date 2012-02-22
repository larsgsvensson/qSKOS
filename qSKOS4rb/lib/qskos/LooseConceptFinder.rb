require_relative 'LoggingRdfReader'
require_relative 'SKOSUtils'

=begin rdoc
Generates a list of SKOS concepts (identified by URI) that don't have any SKOS relationships to other concepts.
=end

class LooseConceptFinder

	def initialize(loggingRdfReader, log, allConcepts)
		log.info("identifying loose concepts")

		@reader = loggingRdfReader
		@log = log
		@looseConcepts = Array.new(allConcepts)

		findLooseConcepts
	end
	
	def getLooseConcepts
		@looseConcepts
	end

	private

	def findLooseConcepts
		@reader.loopStatements do |statement|
			if (isSkosPredicate(statement.predicate))
				if (@looseConcepts.include?(statement.subject))
					@looseConcepts.delete(statement.subject)
				end
				if (@looseConcepts.include?(statement.object))
					@looseConcepts.delete(statement.object)
				end
			end
		end
	end

	def isSkosPredicate(predicate)
		return SKOSUtils.instance.inSkosNamespace?(predicate)
	end

end