package com.googlecode.contraildb.core.query;

import com.googlecode.lingwah.Grammar;
import com.googlecode.lingwah.Parser;
import com.googlecode.lingwah.parser.ChoiceParser;

/**
 * This grammar describes the Contrail Query Language
 * @author ted.stockwell
 *
 */
public class ContrailQueryGrammar extends Grammar {
	

	//
	// reserved words
	//
	public final ChoiceParser ReservedWord=choice();
	public final Parser AND= istr("and").optionFor(ReservedWord);
	public final Parser AS= istr("as").optionFor(ReservedWord);
	public final Parser FROM= istr("from").optionFor(ReservedWord);
	public final Parser NULL= istr("null").optionFor(ReservedWord);
	public final Parser OR= istr("or").optionFor(ReservedWord);
	public final Parser SELECT= istr("select").optionFor(ReservedWord);
	public final Parser DISTINCT= istr("DISTINCT").optionFor(ReservedWord);
	public final Parser UPDATE= istr("UPDATE").optionFor(ReservedWord);
	public final Parser SET= istr("SET").optionFor(ReservedWord);

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
	public final Parser SC_OR= str("||").optionFor(Operators);
	public final Parser SC_AND= str("&&").optionFor(Operators);
	public final Parser PLUS= str("+").optionFor(Operators);
	public final Parser MINUS= str("-").optionFor(Operators);
	public final Parser STAR= str("*").optionFor(Operators);
	public final Parser SLASH= str("/").optionFor(Operators);
	public final Parser XOR= str("^").optionFor(Operators);
	public final Parser NOT= str("!").optionFor(Operators);

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
	
	
	//
	// literals
	//
	
	public final Parser Boolean= choice(istr("true"), istr("false"));
	public final Parser StringCharacter= choice(EscapeSequence, InputCharacter.excluding(DOUBLE_QUOTE, str("\\")));
	public final Parser String= seq(DOUBLE_QUOTE, zeroOrMore(StringCharacter), DOUBLE_QUOTE);
	public final Parser Identifier=  IdentifierChars.excluding(Boolean, ReservedWord);

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
	// Clauses
	//

	public final Parser SelectClause= seq(SELECT, opt(DISTINCT), SelectExpression, zeroOrMore(seq(COMMA,SelectExpression)));
	public final Parser SimpleSelectClause= seq(SELECT, opt(DISTINCT), SimpleSelectExpression);
	public final Parser UpdateClause= seq(UPDATE, AbstractSchemaName, opt(AS), AliasIdentificationVariable, SET, UpdateItem, zeroOrMore(seq(COMMA, UpdateItem)));
	public final Parser DeleteClause= "DELETE" ["FROM"] AbstractSchemaName ["AS"] AliasIdentificationVariable
	public final Parser FromClause= "FROM" IdentificationVariableDeclaration {"," IdentificationVariableDeclaration}*
	public final Parser SubselectFromClause= "FROM" SubselectIdentificationVariableDeclaration {"," SubselectIdentificationVariableDeclaration}*
	public final Parser WhereClause= "WHERE" ConditionalExpression
	public final Parser HavingClause= "HAVING" ConditionalExpression
	public final Parser GroupByClause= "GROUP" "BY" GroupByItem {"," GroupByItem}*
	public final Parser OrderByClause= "ORDER" "BY" OrderByItem {"," OrderByItem}*
	public final Parser Subselect= SimpleSelectClause SubselectFromClause [WhereClause] [GroupByClause] [HavingClause] [OrderByClause]
	
		
	//
	// Items
	//

	public final Parser UpdateItem= IdentificationVariable "." (StateField | SingleValuedAssociationField) "=" NewValue
	public final Parser OrderByItem= (ResultVariable | SingleValuedPathExpression) ["ASC" | "DESC"]
	public final Parser GroupByItem= IdentificationVariable | SingleValuedPathExpression
	public final Parser NewValue= ScalarExpression | SimpleEntityExpression | "NULL"
	
	//
	// From, Join and Index by
	//

	public final Parser IdentificationVariableDeclaration= RangeVariableDeclaration [IndexBy] {JoinVariableDeclaration}*
	public final Parser SubselectIdentificationVariableDeclaration= IdentificationVariableDeclaration | (AssociationPathExpression ["AS"] AliasIdentificationVariable)
	public final Parser JoinVariableDeclaration= Join [IndexBy]
	public final Parser RangeVariableDeclaration= AbstractSchemaName ["AS"] AliasIdentificationVariable
	public final Parser Join= ["LEFT" ["OUTER"] | "INNER"] "JOIN" JoinAssociationPathExpression
	                                               ["AS"] AliasIdentificationVariable ["WITH" ConditionalExpression]
	public final Parser IndexBy= "INDEX" "BY" SimpleStateFieldPathExpression
	
	//
	// Select Expressions
	//

	public final Parser SelectExpression= IdentificationVariable | PartialObjectExpression | (AggregateExpression | "(" Subselect ")"  | FunctionDeclaration | ScalarExpression) [["AS"] AliasResultVariable]
	public final Parser SimpleSelectExpression= ScalarExpression | IdentificationVariable |
	                            (AggregateExpression [["AS"] AliasResultVariable])
	public final Parser PartialObjectExpression= "PARTIAL" IdentificationVariable "." PartialFieldSet
	public final Parser PartialFieldSet= "{" SimpleStateField {"," SimpleStateField}* "}"
	
	//
	// Conditional Expressions
	//

	public final Parser ConditionalExpression= ConditionalTerm {"OR" ConditionalTerm}*
	public final Parser ConditionalTerm= ConditionalFactor {"AND" ConditionalFactor}*
	public final Parser ConditionalFactor= ["NOT"] ConditionalPrimary
	public final Parser ConditionalPrimary= SimpleConditionalExpression | "(" ConditionalExpression ")"
	public final Parser SimpleConditionalExpression= ComparisonExpression | BetweenExpression | LikeExpression |
	                                InExpression | NullComparisonExpression | ExistsExpression |
	                                EmptyCollectionComparisonExpression | CollectionMemberExpression |
	                                InstanceOfExpression
	   
	//
	// Collection Expressions
    //	                                

	public final Parser EmptyCollectionComparisonExpression= CollectionValuedPathExpression "IS" ["NOT"] "EMPTY"
	public final Parser CollectionMemberExpression= EntityExpression ["NOT"] "MEMBER" ["OF"] CollectionValuedPathExpression
	
	// 
	// Literal Values
	//

	public final Parser Literal= string | char | integer | float | boolean
	public final Parser InParameter= Literal | InputParameter
	
	//
	// Input Parameter
	//

	public final Parser InputParameter= PositionalParameter | NamedParameter
	public final Parser PositionalParameter= "?" integer
	public final Parser NamedParameter= ":" string
	
	//
	// Arithmetic Expressions
	//

	public final Parser ArithmeticExpression= SimpleArithmeticExpression | "(" Subselect ")"
	public final Parser SimpleArithmeticExpression= ArithmeticTerm {("+" | "-") ArithmeticTerm}*
	public final Parser ArithmeticTerm= ArithmeticFactor {("*" | "/") ArithmeticFactor}*
	public final Parser ArithmeticFactor= [("+" | "-")] ArithmeticPrimary
	public final Parser ArithmeticPrimary= SingleValuedPathExpression | Literal | "(" SimpleArithmeticExpression ")"
	                               | FunctionsReturningNumerics | AggregateExpression | FunctionsReturningStrings
	                               | FunctionsReturningDatetime | IdentificationVariable | InputParameter | CaseExpression

    //
	// Scalar and Type Expressions
	//

	public final Parser ScalarExpression= SimpleArithmeticExpression | StringPrimary | DateTimePrimary | StateFieldPathExpression
	                           BooleanPrimary | EntityTypeExpression | CaseExpression
	public final Parser StringExpression= StringPrimary | "(" Subselect ")"
	public final Parser StringPrimary= StateFieldPathExpression | string | InputParameter | FunctionsReturningStrings | AggregateExpression | CaseExpression
	public final Parser BooleanExpression= BooleanPrimary | "(" Subselect ")"
	public final Parser BooleanPrimary= StateFieldPathExpression | boolean | InputParameter
	public final Parser EntityExpression= SingleValuedAssociationPathExpression | SimpleEntityExpression
	public final Parser SimpleEntityExpression= IdentificationVariable | InputParameter
	public final Parser DatetimeExpression= DatetimePrimary | "(" Subselect ")"
	public final Parser DatetimePrimary= StateFieldPathExpression | InputParameter | FunctionsReturningDatetime | AggregateExpression

	//
	// Aggregate Expressions
	//

	public final Parser AggregateExpression= ("AVG" | "MAX" | "MIN" | "SUM") "(" ["DISTINCT"] StateFieldPathExpression ")" |
	                        "COUNT" "(" ["DISTINCT"] (IdentificationVariable | SingleValuedPathExpression) ")"
	   
	//
	// Other Expressions
	// 

	public final Parser QuantifiedExpression= ("ALL" | "ANY" | "SOME") "(" Subselect ")"
	public final Parser BetweenExpression= ArithmeticExpression ["NOT"] "BETWEEN" ArithmeticExpression "AND" ArithmeticExpression;
	public final Parser ComparisonExpression= ArithmeticExpression ComparisonOperator ( QuantifiedExpression | ArithmeticExpression );
	public final Parser InExpression= StateFieldPathExpression ["NOT"] "IN" "(" (InParameter {"," InParameter}* | Subselect) ")";
	public final Parser InstanceOfExpression= IdentificationVariable ["NOT"] "INSTANCE" ["OF"] (InstanceOfParameter | "(" InstanceOfParameter {"," InstanceOfParameter}* ")");
	public final Parser InstanceOfParameter= AbstractSchemaName | InputParameter;
	public final Parser LikeExpression= StringExpression ["NOT"] "LIKE" string ["ESCAPE" char];
	public final Parser NullComparisonExpression= (SingleValuedPathExpression | InputParameter) "IS" ["NOT"] "NULL";
	public final Parser ExistsExpression= ["NOT"] "EXISTS" "(" Subselect ")";
	public final Parser ComparisonOperator= "=" | "<" | "<=" | "<>" | ">" | ">=" | "!=";
	
	//
	// Functions
	//

	public final Parser FunctionDeclaration= FunctionsReturningStrings | FunctionsReturningNumerics | FunctionsReturningDateTime;

	public final Parser FunctionsReturningNumerics=
	        "LENGTH" "(" StringPrimary ")" |
	        "LOCATE" "(" StringPrimary "," StringPrimary ["," SimpleArithmeticExpression]")" |
	        "ABS" "(" SimpleArithmeticExpression ")" | "SQRT" "(" SimpleArithmeticExpression ")" |
	        "MOD" "(" SimpleArithmeticExpression "," SimpleArithmeticExpression ")" |
	        "SIZE" "(" CollectionValuedPathExpression ")";

	public final Parser FunctionsReturningDateTime= "CURRENT_DATE" | "CURRENT_TIME" | "CURRENT_TIMESTAMP";

	public final Parser FunctionsReturningStrings=
	        "CONCAT" "(" StringPrimary "," StringPrimary ")" |
	        "SUBSTRING" "(" StringPrimary "," SimpleArithmeticExpression "," SimpleArithmeticExpression ")" |
	        "TRIM" "(" [["LEADING" | "TRAILING" | "BOTH"] [char] "FROM"] StringPrimary ")" |
	        "LOWER" "(" StringPrimary ")" |
	        "UPPER" "(" StringPrimary ")";
	    	
	public final Parser SelectStatement= seq(SelectClause, opt(FromClause), opt(WhereClause), opt(GroupByClause), opt(HavingClause), opt(OrderByClause));
	public final Parser UpdateStatement= seq(UpdateClause, opt(WhereClause));
	public final Parser DeleteStatement= seq(DeleteClause, opt(WhereClause));
	        
	public final Parser QueryLanguage= choice(SelectStatement, UpdateStatement, DeleteStatement);
	        
}
