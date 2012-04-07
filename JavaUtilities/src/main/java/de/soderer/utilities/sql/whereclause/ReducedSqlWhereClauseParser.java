package de.soderer.utilities.sql.whereclause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.soderer.utilities.sql.whereclause.token.Expression;
import de.soderer.utilities.sql.whereclause.token.ListExpression;
import de.soderer.utilities.sql.whereclause.token.Operator;
import de.soderer.utilities.sql.whereclause.token.RulePart;
import de.soderer.utilities.sql.whereclause.token.SuplementalPart;
import de.soderer.utilities.sql.whereclause.token.Value;
import de.soderer.utilities.sql.whereclause.token.Value.Type;

/**
 * Parser for SQL Where-clauses.
 * The parsed has NOT full SQL-Standard functionality.
 * The parsed clause can be output formatted as Oracle, MySQL or BeanShell string.
 * 
 * Supported functionality:
 *	String-values ( 'abc' )
 *	Number-values ( 1, 2.5 )
 *	Date-values ( date('01.02.2001', 'dd.mm.yyyy') )
 *	Date-conversions ( char(datefield, 'dd.mm.yyyy') )
 *	Systemtime-values ( sysdate )
 *	Equations and Comparisons ( =, <, >, >=, <=, !=, <> )
 *	Regular Expressions ( ... like '...' / ... not like '...' )
 *	NullChecks ( ... is null / ... is not null )
 *	ListChecks ( ... in (1, 2, 3) )
 *	Boolean operators ( and, or )
 *	Brackets around expressions
 *	Modulo operator ( ... mod 3 = 0 )
 * 
 * @author Andreas
 * 
 */
public class ReducedSqlWhereClauseParser {
	public RulePart parse(String formula, Map<String, Value.Type> fieldDescriptors2) {
		Map<String, Value.Type> fieldDescriptors = new HashMap<String, Value.Type>(fieldDescriptors2);
		for (String value : Expression.SYSDATE_VALUES) {
			fieldDescriptors.put(value, Value.Type.Date);
		}
		
		// Index des Chars, der zurzeit betrachtet wird.
		int charIndex = 0;
		int length = formula.length();
		String lowerFormula = formula.toLowerCase();
		List<RulePart> parts = new ArrayList<RulePart>();
		boolean readingString = false;
		StringBuilder currentString = new StringBuilder();
		
		// Alle Operatoren
		Set<String> operators = new HashSet<String>();
		operators.addAll(Expression.BOOL_UNARY_POSTFIX_OPERATOR_SIGNS);
		operators.addAll(Expression.COMPARE_OPERATOR_SIGNS);
		operators.addAll(Expression.STRING_ONLY_COMPARE_OPERATOR_SIGNS);
		operators.addAll(Expression.BOOL_ONLY_OPERATOR_SIGNS);
		operators.addAll(Expression.NUMBER_ONLY_OPERATOR_SIGNS);
		operators.addAll(Expression.DATE_FUNCTION_SIGNS);
		operators.addAll(Expression.CHAR_FUNCTION_SIGNS);
		operators.addAll(ListExpression.SIGNS);
		
		// Alle Strings die eine Bedeutung haben
        Set<String> texts = new HashSet<String>();
        texts.addAll(operators);
        texts.addAll(fieldDescriptors.keySet());
        
        int openBrackets = 0;

		while (charIndex < length) {
			char current = formula.charAt(charIndex);

			if (current == '\'') {
				if (charIndex + 1 < length && formula.charAt(charIndex + 1) == '\'') {
					if (readingString) {
						// Doppeltes Quote in einem String ist die Escapesequenz f�r ein Quote im String
						currentString.append(current);
						charIndex += 2;
					} else if (charIndex + 2 < length && formula.charAt(charIndex + 2) == '\'')  {
						// Tripple Quote au�erhalb eines Strings bedeutet ein String der mit einem escapten Quote beginnt
						currentString.append(current);
						readingString = true;
						charIndex += 3;
					} else {
						// Doppeltes Quote au�erhalb eines Strings bedeutet ein Empty String
						parts.add(new Value(""));
						charIndex += 2;
					}
				} else {
					// Einfaches Quote startet oder beendet einen String
					readingString = !readingString;
					if (!readingString) {
						// Stringende
						parts.add(new Value(currentString.toString()));
						currentString = new StringBuilder();
					}
					charIndex++;
				}
			} else if (readingString) {
				currentString.append(current);
				charIndex++;   
			} else if (current == ','){
                parts.add(new SuplementalPart(SuplementalPart.Type.Separator));
                charIndex++;
			} else if (!Character.isWhitespace(current)) {
				if (current == '(') {
					parts.add(new SuplementalPart(SuplementalPart.Type.OpeningBracket));
					openBrackets++;
					charIndex++;
				} else if (current == ')') {
					parts.add(new SuplementalPart(SuplementalPart.Type.ClosingBracket));
					openBrackets--;
					if (openBrackets < 0) {
						throw new IllegalArgumentException("Too many closing brackets");
					}
					charIndex++;
				} else if (Character.isDigit(current) || current == '.') {
					// Beginn einer Zahl, die komplett ausgelesen wird
					int end = charIndex + 1;
					boolean pointSeen = current == '.';

					while (end < length) {
						char next = formula.charAt(end);
						if (Character.isDigit(next)) {
							end++;
						} else if (next == '.' && !pointSeen) {
							pointSeen = true;
							end++;
						} else
							break;
					}

					parts.add(new Value(Double.parseDouble(formula.substring(charIndex, end))));
					charIndex = end;
				} else {
					// Ein Platzhalter f�r eine Operation, Funktion oder Konstante wird gelesen.
					int bestLength = 0;

					for (String check : texts) {
						if (lowerFormula.startsWith(check, charIndex)) {
							if (check.length() > bestLength) {
								// der l�ngste m�gliche String wird erkannt
								bestLength = check.length();
							}
						}
					}

					if (bestLength == 0) {
						// String gefunden der keinem bekannten Operator oder Field entspricht
						throw new IllegalArgumentException("Invalid data found at position " + charIndex + " starting with " + current);
					} else {
						String value = formula.substring(charIndex, charIndex + bestLength);
						if (operators.contains(value.toLowerCase())) {
							parts.add(new Operator(value));
						} else if (fieldDescriptors.keySet().contains(value.toLowerCase())) {
							parts.add(new Value(value, fieldDescriptors.get(value.toLowerCase())));
						}
						charIndex += bestLength;
					}
				}
			} else {
				charIndex++;
			}
		}

		if (openBrackets > 0) {
			throw new IllegalArgumentException("Too many opening brackets");
		} else {
			boolean changed = true;
			while (parts.size() > 1 && changed) {
				changed = false;
				changed = parseLevel1Expressions(parts) || changed;
				changed = parseLevel2Expressions(parts) || changed;
				changed = parseConditions(parts) || changed;
				changed = removeUnusedBrackets(parts) || changed;
			}
			
			if (parts.size() > 1 || !(parts.get(0) instanceof Value) || ((Value)parts.get(0)).type != Value.Type.Bool) {
				throw new IllegalArgumentException("Rule cannot be evaluated");
			} else {
				return parts.get(0);
			}
		}
	}
	
	private boolean parseLevel1Expressions(List<RulePart> parts) {
		boolean changed = false;
		for (int i = parts.size() - 1; i >= 0; i--) {
			if (parts.get(i) instanceof Operator) {
				Operator operator = (Operator)parts.get(i);
				if (Expression.NUMBER_ONLY_OPERATOR_SIGNS.contains(operator.sign)) {
						if (i - 1 < 0 || i + 1 >= parts.size()
							|| !(parts.get(i - 1) instanceof Value)
							|| !(parts.get(i + 1) instanceof Value)) {
							throw new IllegalArgumentException("Invalid definition for operator: " + operator.sign);
						} else {
							Expression newValue = new Expression(((Value)(parts.get(i - 1))), operator, ((Value)(parts.get(i + 1))));
							parts.remove(i + 1);
							parts.remove(i);
							parts.set(i - 1, newValue);
							changed = true;
						}
				} else if ((Expression.DATE_FUNCTION_SIGNS.contains(operator.sign)
					|| Expression.CHAR_FUNCTION_SIGNS.contains(operator.sign))) {
					if (i + 5 >= parts.size()
						|| !(parts.get(i + 1) instanceof SuplementalPart) || ((SuplementalPart)parts.get(i + 1)).type != SuplementalPart.Type.OpeningBracket
						|| !(parts.get(i + 2) instanceof Value)
						|| !(parts.get(i + 3) instanceof SuplementalPart) || ((SuplementalPart)parts.get(i + 3)).type != SuplementalPart.Type.Separator
						|| !(parts.get(i + 4) instanceof Value)
						|| !(parts.get(i + 5) instanceof SuplementalPart) || ((SuplementalPart)parts.get(i + 5)).type != SuplementalPart.Type.ClosingBracket) {
						throw new IllegalArgumentException("Invalid definition for function: " + operator.sign);
					} else {
						Expression newValue = new Expression(operator, ((Value)(parts.get(i + 2))), ((Value)(parts.get(i + 4))));
						parts.remove(i + 5);
						parts.remove(i + 4);
						parts.remove(i + 3);
						parts.remove(i + 2);
						parts.remove(i + 1);
						parts.set(i, newValue);
						changed = true;
					}
				} else if (Expression.BOOL_UNARY_POSTFIX_OPERATOR_SIGNS.contains(operator.sign)) {
					if (i - 1 < 0
						|| !(parts.get(i - 1) instanceof Value)) {
						throw new IllegalArgumentException("Invalid definition for unary operator: " + operator.sign);
					} else {
						Expression newValue = new Expression(((Value)(parts.get(i - 1))), operator);
						parts.remove(i);
						parts.set(i - 1, newValue);
						changed = true;
					}
				}
			}
		}
		return changed;
	}
	
	private boolean parseLevel2Expressions(List<RulePart> parts) {
		boolean changed = false;
		for (int i = parts.size() - 1; i >= 0; i--) {
			if (parts.get(i) instanceof Operator) {
				Operator operator = (Operator)parts.get(i);
				if (Expression.STRING_ONLY_COMPARE_OPERATOR_SIGNS.contains(operator.sign)
					|| Expression.COMPARE_OPERATOR_SIGNS.contains(operator.sign)) {
					if (i - 1 < 0 || i + 1 >= parts.size()
						|| !(parts.get(i - 1) instanceof Value)
						|| !(parts.get(i + 1) instanceof Value)) {
						throw new IllegalArgumentException("Invalid definition for operator: " + operator.sign);
					} else {
						Expression newValue = new Expression(((Value)(parts.get(i - 1))), operator, ((Value)(parts.get(i + 1))));
						parts.remove(i + 1);
						parts.remove(i);
						parts.set(i - 1, newValue);
						changed = true;
					}
				} else if (ListExpression.SIGNS.contains(operator.sign)) {
					if (i - 1 < 0
						|| !(parts.get(i - 1) instanceof Value)
						|| !(parts.get(i + 1) instanceof SuplementalPart) || ((SuplementalPart)parts.get(i + 1)).type != SuplementalPart.Type.OpeningBracket) {
						throw new IllegalArgumentException("Invalid definition for list operator: " + operator.sign);
					} else {
						// search next closing bracket
						int closingPosition = -1;
						List<Value> values = new ArrayList<Value>();
						for (int j = i + 2; j < parts.size(); j++) {
							if (parts.get(j) instanceof SuplementalPart && ((SuplementalPart)parts.get(j)).type == SuplementalPart.Type.ClosingBracket) {
								closingPosition = j;
								break;
							} else if (parts.get(j) instanceof Value) {
								values.add((Value)parts.get(j));
							} else if (!(parts.get(j) instanceof SuplementalPart) || ((SuplementalPart)parts.get(j)).type != SuplementalPart.Type.Separator) {
								throw new IllegalArgumentException("Invalid list definition");
							}
						}
						
						if (closingPosition < 0) {
							throw new IllegalArgumentException("Invalid list definition");
						} else {
							ListExpression newValue = new ListExpression((Value)parts.get(i - 1), (Operator)parts.get(i), values);
							for (int j = closingPosition; j > i; j--) {
								parts.remove(j);
							}
							parts.remove(i);
							parts.set(i - 1, newValue);
						}
					}
				}
			}
		}
		return changed;
	}
	
	private boolean removeUnusedBrackets(List<RulePart> parts) {
		boolean changed = false;
		for (int i = parts.size() - 1; i >= 0; i--) {
			if (parts.get(i) instanceof SuplementalPart && ((SuplementalPart)parts.get(i)).type == SuplementalPart.Type.OpeningBracket) {
				if (i + 1 < parts.size() && parts.get(i) instanceof SuplementalPart && ((SuplementalPart)parts.get(i)).type == SuplementalPart.Type.ClosingBracket) {
					parts.remove(i + 1);
					parts.remove(i);
					changed = true;
				} else if (i + 2 < parts.size() && parts.get(i + 2) instanceof SuplementalPart && ((SuplementalPart)parts.get(i + 2)).type == SuplementalPart.Type.ClosingBracket) {
					parts.remove(i + 2);
					parts.remove(i);
					changed = true;
				}
			}
		}
		return changed;
	}
	
	private boolean parseConditions(List<RulePart> parts) {
		boolean changed = false;
		int bracketlevel = -1;
		int arithmeticLevel = -1;
		int currentBracketLevel = 0;
		int position = -1;
		for (int i = parts.size() - 1; i >= 0; i--) {
			if ((parts.get(i) instanceof SuplementalPart)) {
				if (((SuplementalPart)parts.get(i)).type == SuplementalPart.Type.OpeningBracket) {
					currentBracketLevel--;
				} else if (((SuplementalPart)parts.get(i)).type == SuplementalPart.Type.ClosingBracket) {
					currentBracketLevel++;
				}
			} else if (parts.get(i) instanceof Operator && Expression.BOOL_ONLY_OPERATOR_SIGNS.contains(((Operator)parts.get(i)).sign.toLowerCase())) {
				if (i - 1 >= 0 && i + 1 < parts.size()
						&& parts.get(i - 1) instanceof Value && ((Value)parts.get(i - 1)).type == Type.Bool
						&& parts.get(i + 1) instanceof Value && ((Value)parts.get(i + 1)).type == Type.Bool) {
					int nextArithmeticLevel = Expression.BOOL_ONLY_OPERATOR_SIGNS.indexOf(((Operator)parts.get(i)).sign);
					if (bracketlevel < currentBracketLevel || (currentBracketLevel == bracketlevel && arithmeticLevel <= nextArithmeticLevel)) {
						arithmeticLevel = nextArithmeticLevel;
						bracketlevel = currentBracketLevel;
						position = i;
					}
				}
			}
		}
		
		if (position >= 0) {
			Expression newValue = new Expression((Value)parts.get(position - 1), (Operator)parts.get(position), (Value)parts.get(position + 1));
			parts.remove(position + 1);
			parts.remove(position);
			parts.set(position - 1, newValue);
			changed = true;
		}
		return changed;
	}
	
	/**
	 * Debug Helper Method
	 * 
	 * @param tokens
	 */
	public static void printJoinedParts(List<RulePart> tokens) {
		StringBuilder countBuilder = new StringBuilder();
		StringBuilder builder = new StringBuilder();
		for (Object token : tokens) {
			if (builder.length() > 0) {
				countBuilder.append(" ");
				builder.append(" ");
			}
			builder.append(token);
			countBuilder.append((tokens.indexOf(token) % 10) + StringUtils.repeat(" ", token.toString().length() - 1));
		}
		System.out.println(countBuilder);
		System.out.println(builder);
	}
}
