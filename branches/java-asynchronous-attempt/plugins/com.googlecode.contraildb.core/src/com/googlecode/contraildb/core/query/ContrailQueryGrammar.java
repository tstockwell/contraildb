package com.googlecode.contraildb.core.query;

import com.googlecode.lingwah.Grammar;
import com.googlecode.lingwah.Parser;
import com.googlecode.lingwah.parser.ChoiceParser;
import com.googlecode.lingwah.parser.ParserReference;
import com.googlecode.lingwah.parser.SpacedSequenceParser;
import com.googlecode.lingwah.parser.common.DecimalParser;
import com.googlecode.lingwah.parser.common.IntegerParser;

/**
 * This grammar describes the Contrail Query Language
 * @author ted.stockwell
 *
 */
public class ContrailQueryGrammar extends Grammar {
	public final ParserReference SubSelect= ref();
	

	//
	// reserved words
	//
	public final ChoiceParser ReservedWord=choice();
	public final Parser AND= istr("AND").optionFor(ReservedWord);
	public final Parser AS= istr("AS").optionFor(ReservedWord);
	public final Parser FROM= istr("FROM").optionFor(ReservedWord);
	public final Parser NULL= istr("NULL").optionFor(ReservedWord);
	public final Parser OR= istr("OR").optionFor(ReservedWord);
	public final Parser SELECT= istr("SELECT").optionFor(ReservedWord);
	public final Parser DISTINCT= istr("DISTINCT").optionFor(ReservedWord);
	public final Parser UPDATE= istr("UPDATE").optionFor(ReservedWord);
	public final Parser SET= istr("SET").optionFor(ReservedWord);
	public final Parser DELETE= istr("DELETE").optionFor(ReservedWord);
	public final Parser WHERE= istr("WHERE").optionFor(ReservedWord);
	public final Parser GROUP= istr("GROUP").optionFor(ReservedWord);
	public final Parser BY= istr("BY").optionFor(ReservedWord);
	public final Parser HAVING= istr("HAVING").optionFor(ReservedWord);
	public final Parser ORDER= istr("ORDER").optionFor(ReservedWord);
	public final Parser ASC= istr("ASC").optionFor(ReservedWord);
	public final Parser DESC= istr("DESC").optionFor(ReservedWord);
	public final Parser LEFT= istr("LEFT").optionFor(ReservedWord);
	public final Parser OUTER= istr("OUTER").optionFor(ReservedWord);
	public final Parser INNER= istr("INNER").optionFor(ReservedWord);
	public final Parser WITH= istr("WITH").optionFor(ReservedWord);
	public final Parser JOIN= istr("JOIN").optionFor(ReservedWord);
	public final Parser INDEX= istr("INDEX").optionFor(ReservedWord);
	public final Parser PARTIAL= istr("PARTIAL").optionFor(ReservedWord);
	public final Parser IS= istr("IS").optionFor(ReservedWord);
	public final Parser EMPTY= istr("EMPTY").optionFor(ReservedWord);
	public final Parser MEMBER= istr("MEMBER").optionFor(ReservedWord);
	public final Parser OF= istr("OF").optionFor(ReservedWord);
	public final Parser EXISTS= istr("EXISTS").optionFor(ReservedWord);
	public final Parser ALL= istr("ALL").optionFor(ReservedWord);
	public final Parser ANY= istr("ANY").optionFor(ReservedWord);
	public final Parser SOME= istr("SOME").optionFor(ReservedWord);
	public final Parser IN= istr("IN").optionFor(ReservedWord);
	public final Parser BETWEEN= istr("BETWEEN").optionFor(ReservedWord);
	public final Parser LIKE= istr("LIKE").optionFor(ReservedWord);
	public final Parser ESCAPE= istr("ESCAPE").optionFor(ReservedWord);

	//
	// Operators
	//
	public final ChoiceParser Operators= choice();
	public final Parser ASSIGN= str("=").optionFor(Operators);
	public final Parser GT= str(">").optionFor(Operators);
	public final Parser LT= str("<").optionFor(Operators);
	public final Parser EQ= str("==").optionFor(Operators);
	public final Parser LE= str("<=").optionFor(Operators);
	public final Parser GE= str(">=").optionFor(Operators);
	public final Parser NE= str("!=").optionFor(Operators);
	public final Parser LTGT= str("<>").optionFor(Operators);
	public final Parser SC_OR= str("||").optionFor(Operators);
	public final Parser SC_AND= str("&&").optionFor(Operators);
	public final Parser PLUS= str("+").optionFor(Operators);
	public final Parser MINUS= str("-").optionFor(Operators);
	public final Parser STAR= str("*").optionFor(Operators);
	public final Parser SLASH= str("/").optionFor(Operators);
	public final Parser XOR= str("^").optionFor(Operators);
	public final Parser NOT= str("!").optionFor(Operators);
	public final Parser QUESTION_MARK= str("?").optionFor(Operators);
	public final Parser SEMICOLON= str(":").optionFor(Operators);

	//
	// Punctuation
	//
	public final Parser LPAREN= str("("); 
	public final Parser RPAREN= str(")");  
	public final Parser LBRACE= str("{"); 
	public final Parser RBRACE= str("}");  
	public final Parser LBRACKET= str("[");  
	public final Parser RBRACKET= str("]"); 
	public final Parser COMMA= str(","); 
	public final Parser DOT= str("."); 
	public final Parser COLON= str(":"); 
	public final Parser DOUBLE_QUOTE= str("\"");
	public final Parser SINGLE_QUOTE= str("\'");
	public final Parser BACKSLASH= str("\\");
	


	public final Parser LineTerminator= choice(str("\n"), str("\r"), str("\n\r"), str("\r\n")); 
	public final Parser InputCharacter= excluding(anyChar(), LineTerminator);
	public final Parser EscapedCharacter= choice(SINGLE_QUOTE, DOUBLE_QUOTE, str("b"), str("f"), str("t"), str("n"), str("\\"));
	public final Parser EscapeSequence= seq(BACKSLASH, EscapedCharacter);
	public final Parser NonzeroDigit= range('1', '9');
	public final Parser Digit= choice(str('0'), NonzeroDigit);
	public final Parser NameCharacter= choice(excluding(InputCharacter, SINGLE_QUOTE, BACKSLASH), EscapeSequence);
	public final Parser IdentifierLetter= choice(range('a', 'z'), range('A', 'Z'), str('_'));
	public final Parser IdentifierLetterOrDigit= choice(IdentifierLetter, Digit);
	public final Parser IdentifierChars= seq(IdentifierLetter, zeroOrMore(IdentifierLetterOrDigit));
	
	// comments
   	public final Parser EndOfLineComment= seq(str("//"), zeroOrMore(anyChar()), LineTerminator);
   	public final Parser InLineComment= seq(str("/*"), zeroOrMore(anyChar()), str("*/"));
	public final Parser DocumentationComment= seq(str("/**"), zeroOrMore(anyChar()), str("*/"));
	
	
	// whitespace
	public final Parser ws= oneOrMore(choice(str(" "), str("\t"), str("\f"), LineTerminator, InLineComment));
	public final Parser optws= opt(ws); 
	/**
	 * Creates a sequence matcher that expects white space between all the 
	 * elements 
	 */
	public final Parser sseq(Parser...matchers) {
		return new SpacedSequenceParser(optws, matchers);
	}
	public final Parser CommaSeparator= seq(optws, COMMA, optws);
	public final Parser DotSeparator=  seq(optws, DOT, optws);
	public final Parser OrSeparator=  seq(optws, OR, optws);
	public final Parser AndSeparator=  seq(optws, AND, optws);
	
	//
	// literals
	//
	
	public final Parser Boolean= choice(istr("true"), istr("false"));
	public final Parser StringCharacter= choice(EscapeSequence, InputCharacter.excluding(SINGLE_QUOTE, str("\\")));
	public final Parser String= seq(SINGLE_QUOTE, zeroOrMore(StringCharacter), SINGLE_QUOTE);
	public final Parser Char= seq(SINGLE_QUOTE, StringCharacter, SINGLE_QUOTE);
	public final Parser Int= new IntegerParser();
	public final Parser Decimal= new DecimalParser();
	public final Parser Identifier=  IdentifierChars.excluding(Boolean, ReservedWord);
	public final Parser Literal= choice(String, Int, Decimal, Boolean, Char);
	
	//
	// Input Parameter
	//

	public final Parser PositionalParameter= sseq(QUESTION_MARK, Int);
	public final Parser NamedParameter= sseq(SEMICOLON, String);
	public final Parser InputParameter= choice(PositionalParameter, NamedParameter);

//	string (‘foo’, ‘bar’’s house’, ‘%ninja%’, ...)
//	char (‘/’, ‘\’, ‘ ‘, ...)
//	integer (-1, 0, 1, 34, ...)
//	float (-0.23, 0.007, 1.245342E+8, ...)
//	boolean (false, true)
	
							
	//
	// Identifiers
	//

	/* Alias Identification usage (the "u" of "u.name") */
	public final Parser IdentificationVariable= choice(Identifier);

	/* Alias Identification declaration (the "u" of "FROM User u") */
	public final Parser AliasIdentificationVariable= choice(Identifier);

	/* identifier that must be a class name (the "User" of "FROM User u") */
	public final Parser AbstractSchemaName= choice(Identifier);

	/* identifier that must be a field (the "name" of "u.name") */
	/* This is responsible to know if the field exists in Object, no matter if it's a relation or a simple field */
	public final Parser FieldIdentificationVariable= choice(Identifier);

	/* identifier that must be a collection-valued association field (to-many) (the "Phonenumbers" of "u.Phonenumbers") */
	public final Parser CollectionValuedAssociationField = choice(FieldIdentificationVariable);

	/* identifier that must be a single-valued association field (to-one) (the "Group" of "u.Group") */
	public final Parser SingleValuedAssociationField = choice(FieldIdentificationVariable);

	/* identifier that must be an embedded class state field (for the future) */
	public final Parser EmbeddedClassStateField = choice(FieldIdentificationVariable);

	/* identifier that must be a simple state field (name, email, ...) (the "name" of "u.name") */
	/* The difference between this and FieldIdentificationVariable is only semantical, because it points to a single field (not mapping to a relation) */
	public final Parser SimpleStateField = choice(FieldIdentificationVariable);

	/* Alias ResultVariable declaration (the "total" of "COUNT(*) AS total") */
	public final Parser AliasResultVariable= choice(Identifier);

	/* ResultVariable identifier usage of mapped field aliases (the "total" of "COUNT(*) AS total") */
	public final Parser ResultVariable= choice(Identifier);

	//
	// Path Expressions
	//

	/* "name" */
	public final Parser StateField= 
			seq(zeroOrMore(seq(EmbeddedClassStateField,DOT)), SimpleStateField);

	/* "u.Group" */
	public final Parser SingleValuedAssociationPathExpression= 
			seq(IdentificationVariable, DOT, SingleValuedAssociationField);

	/* "u.Group" or "u.Phonenumbers" declarations */
	public final Parser JoinAssociationPathExpression= 
			seq(IdentificationVariable, DOT, choice(CollectionValuedAssociationField, SingleValuedAssociationField));

	/* "u.name" or "u.Group.name" */
	public final Parser StateFieldPathExpression= 
			seq(choice(IdentificationVariable,SingleValuedAssociationPathExpression), DOT, StateField);

	/* "u.Group.Permissions" */
	public final Parser CollectionValuedPathExpression= 
			seq(IdentificationVariable, DOT, zeroOrMore(seq(SingleValuedAssociationField, DOT)), CollectionValuedAssociationField);

	/* "u.Group" or "u.Phonenumbers" usages */
	public final Parser AssociationPathExpression= 
			choice(CollectionValuedPathExpression, SingleValuedAssociationPathExpression);

	/* "u.name" or "u.Group" */
	public final Parser SingleValuedPathExpression= 
			choice(StateFieldPathExpression, SingleValuedAssociationPathExpression);

	/* "u.name" or "u.address.zip" (address = EmbeddedClassStateField) */
	public final Parser SimpleStateFieldPathExpression= 
			seq(IdentificationVariable, DOT, StateField);
	
	
	//
	// Arithmetic Expressions
	//

	public final ParserReference ArithmeticPrimary= ref(); 
	public final Parser ArithmeticFactor= 
			sseq(opt(choice(PLUS, MINUS)), ArithmeticPrimary);
	public final Parser ArithmeticTerm= 
			sseq(ArithmeticFactor, zeroOrMore(sseq(choice(STAR, SLASH), ArithmeticFactor)));
	public final Parser SimpleArithmeticExpression= 
			sseq(ArithmeticTerm, zeroOrMore(sseq(choice(PLUS, MINUS), ArithmeticTerm)));
	public final Parser ArithmeticExpression= 
			choice(SimpleArithmeticExpression, sseq(LPAREN, SubSelect, RPAREN));
	{
		ArithmeticPrimary.define(choice(
				SingleValuedPathExpression,
				Literal,
				sseq(LPAREN, SimpleArithmeticExpression, RPAREN),
				IdentificationVariable, InputParameter));
	}
	
	// 
	// Literal Values
	//
	public final Parser InParameter= choice(Literal, InputParameter);

    //
	// Scalar and Type Expressions
	//

	public final Parser BooleanPrimary= choice(StateFieldPathExpression, Boolean, InputParameter);
	public final Parser StringPrimary= 
			choice(StateFieldPathExpression, String, InputParameter);
	public final Parser DateTimePrimary= choice(StateFieldPathExpression, InputParameter);
	public final Parser SimpleEntityExpression= choice(IdentificationVariable, InputParameter);
	public final Parser EntityExpression= choice(SingleValuedAssociationPathExpression, SimpleEntityExpression);
	public final Parser ScalarExpression= 
		choice(SimpleArithmeticExpression, StringPrimary, DateTimePrimary, StateFieldPathExpression,
	                           BooleanPrimary, EntityExpression);
	public final Parser StringExpression= 
			choice(StringPrimary, sseq(RPAREN, SubSelect, LPAREN));
	public final Parser BooleanExpression= choice(BooleanPrimary, sseq(RPAREN, SubSelect, LPAREN));
	public final Parser DatetimeExpression= choice(DateTimePrimary, sseq(LPAREN, SubSelect, RPAREN));
	   
	//
	// Collection Expressions
	//	                                

	public final Parser EmptyCollectionComparisonExpression= sseq(CollectionValuedPathExpression, IS, opt(NOT), EMPTY);
	public final Parser CollectionMemberExpression= sseq(EntityExpression, opt(NOT), MEMBER, opt(OF), CollectionValuedPathExpression);

	//
	// Other Expressions
	// 

	public final Parser QuantifiedExpression= 
			sseq(choice(ALL, ANY, SOME), sseq(LPAREN, SubSelect, RPAREN));
	public final Parser BetweenExpression= 
			sseq(ArithmeticExpression, opt(NOT), BETWEEN, ArithmeticExpression, AND, ArithmeticExpression);
	public final Parser ComparisonOperator= 
			choice(ASSIGN, LT, LE, GT, GE, NE, LTGT);
	public final Parser ComparisonExpression= 
			sseq(ArithmeticExpression, ComparisonOperator, choice(QuantifiedExpression, ArithmeticExpression ));
	public final Parser InExpression= 
			sseq(StateFieldPathExpression, opt(NOT), IN, LPAREN, choice(oneOrMore(InParameter).separatedBy(CommaSeparator), SubSelect), RPAREN);
	public final Parser LikeExpression= 
			sseq(StringExpression, opt(NOT), LIKE, String, opt(sseq(ESCAPE, Char)));
	public final Parser NullComparisonExpression= 
			sseq(choice(SingleValuedPathExpression, InputParameter), IS, opt(NOT), NULL);
	public final Parser ExistsExpression= 
			sseq(opt(NOT), EXISTS, sseq(LPAREN, SubSelect, RPAREN));
	
	//
	// Conditional Expressions
	//

	public final ParserReference ConditionalExpression= ref();
	public final ParserReference SimpleConditionalExpression= ref(); 
	public final Parser ConditionalPrimary= choice(SimpleConditionalExpression, sseq(RPAREN, ConditionalExpression, LPAREN));
	public final Parser ConditionalFactor= sseq(opt(NOT), ConditionalPrimary);
	public final Parser ConditionalTerm= oneOrMore(ConditionalFactor).separatedBy(AndSeparator);
	{
		ConditionalExpression.define(oneOrMore(ConditionalTerm).separatedBy(OrSeparator));
		SimpleConditionalExpression.define( 
			choice(ComparisonExpression, BetweenExpression , LikeExpression, 
					InExpression, NullComparisonExpression, ExistsExpression, 
					EmptyCollectionComparisonExpression, CollectionMemberExpression));
	}
	
	//
	// Items
	//
	public final Parser NewValue= choice(ScalarExpression, SimpleEntityExpression, NULL);
	public final Parser UpdateItem= sseq(seq(IdentificationVariable, DOT, choice(StateField, SingleValuedAssociationField)), ASSIGN, NewValue);
	public final Parser OrderByItem= sseq(choice(ResultVariable, SingleValuedPathExpression), opt(choice(ASC, DESC)));
	public final Parser GroupByItem= choice(IdentificationVariable, SingleValuedPathExpression);
	
	//
	// Select Expressions
	//

	public final Parser PartialFieldSet= 
			sseq(RBRACE, oneOrMore(SimpleStateField).separatedBy(CommaSeparator), LBRACE);
	public final Parser PartialObjectExpression= 
			sseq(PARTIAL, IdentificationVariable, DOT, PartialFieldSet);
	public final Parser SelectExpression= 
			choice(IdentificationVariable, PartialObjectExpression, seq(choice(sseq(LPAREN, SubSelect, RPAREN), ScalarExpression), opt(sseq(opt(AS), AliasResultVariable))));
	public final Parser SimpleSelectExpression= 
			choice(ScalarExpression, IdentificationVariable);
	
	//
	// From, Join and Index by
	//

	public final Parser IndexBy= 
			sseq(INDEX, BY, SimpleStateFieldPathExpression);
	public final Parser Join= 
			sseq(opt(choice(sseq(LEFT, opt(OUTER)), INNER)), JOIN, JoinAssociationPathExpression, opt(AS), AliasIdentificationVariable, opt(sseq(WITH, ConditionalExpression)));
	public final Parser JoinVariableDeclaration= 
			sseq(Join, opt(IndexBy));
	public final Parser RangeVariableDeclaration= 
			sseq(AbstractSchemaName, opt(AS), AliasIdentificationVariable);
	public final Parser IdentificationVariableDeclaration= 
			sseq(RangeVariableDeclaration, opt(IndexBy), zeroOrMore(JoinVariableDeclaration));
	public final Parser SubselectIdentificationVariableDeclaration= 
			sseq(choice(IdentificationVariableDeclaration , sseq(AssociationPathExpression, opt(AS), AliasIdentificationVariable)));
	
	//
	// Clauses
	//

	public final Parser SelectClause= 
			sseq(SELECT, opt(DISTINCT), SelectExpression, zeroOrMore(sseq(COMMA,SelectExpression)));
	public final Parser SimpleSelectClause= 
			sseq(SELECT, opt(DISTINCT), SimpleSelectExpression);
	public final Parser UpdateClause= 
			sseq(UPDATE, AbstractSchemaName, opt(AS), AliasIdentificationVariable, SET, UpdateItem, zeroOrMore(sseq(COMMA, UpdateItem)));
	public final Parser DeleteClause= 
			sseq(DELETE, opt(FROM), AbstractSchemaName, opt(AS), AliasIdentificationVariable);
	public final Parser FromClause= 
			sseq(FROM, IdentificationVariableDeclaration, zeroOrMore(sseq(COMMA, IdentificationVariableDeclaration)));
	public final Parser SubselectFromClause= 
			sseq(FROM, SubselectIdentificationVariableDeclaration, zeroOrMore(sseq(COMMA, SubselectIdentificationVariableDeclaration)));
	public final Parser WhereClause= 
			sseq(WHERE, ConditionalExpression);
	public final Parser HavingClause= 
			sseq(HAVING, ConditionalExpression);
	public final Parser GroupByClause= 
			sseq(GROUP, BY, GroupByItem, zeroOrMore(sseq(COMMA, GroupByItem)));
	public final Parser OrderByClause= 
			sseq(ORDER, BY, OrderByItem, zeroOrMore(sseq(COMMA, OrderByItem)));
	
	{
		SubSelect.define(
				sseq(SimpleSelectClause, SubselectFromClause, opt(WhereClause), opt(GroupByClause), opt(HavingClause), opt(OrderByClause)));
	}
	    	
	public final Parser SelectStatement= seq(SelectClause, opt(FromClause), opt(WhereClause), opt(GroupByClause), opt(HavingClause), opt(OrderByClause));
	public final Parser UpdateStatement= seq(UpdateClause, opt(WhereClause));
	public final Parser DeleteStatement= seq(DeleteClause, opt(WhereClause));
	        
	public final Parser QueryLanguage= choice(SelectStatement, UpdateStatement, DeleteStatement);
	        
}
