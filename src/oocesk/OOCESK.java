package oocesk;

/**
 @author  Matthew Might 
 @version 1.0
 @since   2012-08-28
 */

import java.util.Comparator;
import java.util.Hashtable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

/* Abstract syntax tree. */

/*- Classes -*/

/**
 * A ClassDef holds a class definition, which consists of a class name, a parent
 * class name, a table of fields and a table of methods.
 */
class ClassDef {

  /**
   * The name of the class.
   */
  public final String name;

  /**
   * The name of the parent of this class.
   */
  public final String parentClassName;

  private final Hashtable<String, MethodDef> methods = new Hashtable<String, MethodDef>();

  private final Hashtable<String, FieldDef> fields = new Hashtable<String, FieldDef>();

  /**
   * Creates a new class definition.
   * 
   * @param name
   *          the name of the class
   * @param parentClassName
   *          the name of the parent of this class
   */
  public ClassDef(String name, String parentClassName) {
    this.name = name;
    this.parentClassName = parentClassName;
    classMap.put(name, this);
  }

  /**
   * Checks if this class is a sub-class of another.
   * 
   * @return true iff this class is a sub-class of the argument.
   */
  public boolean isInstanceOf(String otherClassName) {
    if (this.name.equals(otherClassName))
      return true;

    if (this.parentClass() == null)
      return false;

    return this.parentClass().isInstanceOf(otherClassName);
  }

  /**
   * Looks up the parent class definition of this class.
   * 
   * @return the parent class definition of this class
   */
  public ClassDef parentClass() {
    return classMap.get(parentClassName);
  }

  /**
   * Returns the right method, possible searching super-classes.
   * 
   * @param methodName
   *          the name of the method to look up
   * @return the method definition
   */
  public MethodDef lookupMethod(String methodName) {
    // Check locally first:
    if (methods.containsKey(methodName))
      return methods.get(methodName);

    // Recur in the parent:
    if (parentClassName != null) {
      ClassDef par = parentClass();
      return par == null ? null : par.lookupMethod(methodName);
    } else {
      throw new RuntimeException("no such method: " + methodName);
    }
  }

  /**
   * Returns the right field, possible searching super-classes.
   * 
   * @param fieldName
   *          the name of the field to look up
   * @return the field definition
   */
  public FieldDef lookupField(String fieldName) {
    // Check locally first:
    if (fields.containsKey(fieldName))
      return fields.get(fieldName);

    // Recur in the parent:
    if (parentClassName != null) {
      ClassDef par = parentClass();
      return par == null ? null : par.lookupField(fieldName);
    } else {
      throw new RuntimeException("no such field: " + fieldName);
    }
  }

  /**
   * Adds a method to this class.
   * 
   * @param methodName
   *          the name of the method to add
   * @param formals
   *          the formal arguments to the method
   * @param body
   *          the initial statement of the method
   */
  public void addMethod(String methodName, String[] formals, Stmt body) {
    MethodDef m = new MethodDef(methodName, formals, body);
    methods.put(methodName, m);
  }

  /**
   * Adds a field to this class.
   * 
   * @param fieldName
   *          the name of the field to add
   */
  public void addField(String fieldName) {
    FieldDef f = new FieldDef(fieldName);
    fields.put(fieldName, f);
  }

  /* The global class database. */
  private static Hashtable<String, ClassDef> classMap = new Hashtable<String, ClassDef>();

  /**
   * Look up a class based on its name.
   * 
   * @param className
   *          the name of the class to look up
   * @return the class with the given name
   */
  public static ClassDef forName(String className) {
    return classMap.get(className);
  }
}

/**
 * A method definition.
 */
class MethodDef {

  /**
   * The name of the method.
   */
  public final String name;

  /**
   * The formal parameters for the method.
   */
  public final String[] formals;

  /**
   * The body of the method (as the initial statement).
   */
  public final Stmt body;

  /**
   * Constructs a new method definition.
   */
  public MethodDef(String name, String[] formals, Stmt body) {
    this.name = name;
    this.formals = formals;
    this.body = body;
  }
}

/**
 * A field definition.
 */
class FieldDef {

  /**
   * The name of the field.
   */
  public final String name;

  /**
   * Constructs a new field definition.
   */
  public FieldDef(String name) {
    this.name = name;
  }
}

/*- Statements -*/

/**
 * A statement is an an individual instruction for the machine.
 */
abstract class Stmt {

  /**
   * The syntactic successor of this statement.
   */
  public final Stmt next;

  /**
   * Constructs a new statement.
   * 
   * @param next
   *          the statement that syntactically follows this one
   */
  public Stmt(Stmt next) {
    this.next = next;
  }

  /**
   * Steps through to the next state of execution, assuming this statement is
   * paired with the given frame poiner, a store and a continuation.
   * 
   * @param fp
   *          the current frame pointer
   * @param store
   *          the current store
   * @param kont
   *          the current continuation
   * @return the next state
   */
  public abstract State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont);

  /* Label-to-statement lookup methods. */

  private static Hashtable<String, Stmt> stmtMap = new Hashtable<String, Stmt>();

  /**
   * Registers this statement as at the supplied label.
   * 
   * @param labelName
   *          the label to which this statement should be attached
   */
  protected void register(String labelName) {
    stmtMap.put(labelName, this);
  }

  /**
   * Maps a label back to a statement.
   * 
   * @param labelName
   *          the label to look up
   * @return the statement for the given label
   */
  public static Stmt forLabel(String labelName) {
    return stmtMap.get(labelName);
  }

}

/**
 * A labeled statement with no effect.
 */
final class LabelStmt extends Stmt {

  /**
   * The name of the label.
   */
  public final String label;

  /**
   * Creates a labeled statement.
   */
  public LabelStmt(String label, Stmt next) {
    super(next);
    this.label = label;
    this.register(label);
  }

  /**
   * Skips to the next instruction.
   */
  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // this.next is the syntactic successor
    // of the current instruction:
    return new State(this.next, fp, store, kont);
  }

}

/**
 * A statement whose only effect is to be skipped.
 */
final class SkipStmt extends Stmt {

  /**
   * Creates a skip statement.
   */
  public SkipStmt(Stmt next) {
    super(next);
  }

  /**
   * Skips to the next statement.
   */
  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // this.next is the syntactic successor
    // of the current statement:
    return new State(this.next, fp, store, kont);
  }

}

/**
 * A statement which sends control to the target label.
 */
final class GotoStmt extends Stmt {

  /**
   * The label to which to jump.
   */
  public final String label;

  /**
   * Creates a goto statement.
   */
  public GotoStmt(Stmt next, String label) {
    super(next);
    this.label = label;
  }

  /**
   * Jumps to the given label, leaving all other components the same.
   */
  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    /*
     * Stmt.forLabel(label) yields the statement that has that label.
     */
    return new State(Stmt.forLabel(label), fp, store, kont);
  }
}

/**
 * A statement which branches or fall through based on the value of the
 * conditional.
 */
final class IfStmt extends Stmt {

  /**
   * The label to which to jump if the condition is true.
   */
  public final String label;

  /**
   * The condition te test.
   */
  public final AExp condition;

  /**
   * Creates an if statement.
   */
  public IfStmt(Stmt next, AExp condition, String label) {
    super(next);
    this.label = label;
    this.condition = condition;
  }

  /**
   * Jumps to the target label if the condition is true, falling through
   * otherwise.
   */
  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // Test the condition:
    if (condition.eval(fp, store).toBoolean())
      // if true, jump to the label:
      return new State(Stmt.forLabel(label), fp, store, kont);
    else
      // if not, fall through:
      return new State(this.next, fp, store, kont);
  }
}

/**
 * A statement which assigns the evaluation of an atomic expression into a
 * register.
 */
final class AssignAExpStmt extends Stmt {

  /**
   * The register into which to assign the result.
   */
  public final String lhs;

  /**
   * The expression to evaluate.
   */
  public final AExp rhs;

  /**
   * Creates a new assignment expression.
   */
  public AssignAExpStmt(Stmt next, String lhs, AExp rhs) {
    super(next);
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // Compute the address of the register:
    Addr a = fp.offset(lhs);

    // Evaluate the right-hand side:
    Value val = rhs.eval(fp, store);

    // Bind the result in the store:
    ImmutableMap<Addr, Value> store_ = Store.extend(store, a, val);

    // Construct the new state:
    return new State(this.next, fp, store_, kont);
  }
}

/**
 * A statement which creates an object of the specified class.
 */
final class NewStmt extends Stmt {

  /**
   * The register in which the new object pointer will reside.
   */
  public final String lhs;

  /**
   * The name of the class for the new object.
   */
  public final String className;

  /**
   * Creates an object allocation statement.
   */
  public NewStmt(Stmt next, String lhs, String className) {
    super(next);
    this.lhs = lhs;
    this.className = className;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Compute the address of the register:
    Addr a = fp.offset(lhs);

    // Construct a new object pointer:
    ObjectPointer op = new ObjectPointer();

    // Construct the object intself:
    ObjectValue object = new ObjectValue(className, op);

    // Bind the register to the object:
    ImmutableMap<Addr, Value> store_ = Store.extend(store, a, object);

    // Construct the new state:
    return new State(this.next, fp, store_, kont);
  }

}

/**
 * An abstract method invocation statement.
 */
abstract class AbstractInvokeStmt extends Stmt {

  /**
   * The register into which the result will be assigned.
   */
  public final String lhs;

  /**
   * The name of the method to call.
   */
  public final String methodName;

  /**
   * The arguments to pass.
   */
  public final AExp[] args;

  public AbstractInvokeStmt(Stmt next, String lhs, String methodName, AExp[] args) {
    super(next);
    this.lhs = lhs;
    this.methodName = methodName;
    this.args = args;
  }

  /**
   * Applies the given method on the specified object.
   */
  protected State applyMethod(MethodDef m, ObjectValue thiss, FramePointer fp,
      ImmutableMap<Addr, Value> store, Kont kont) {

    // Move to the body of the procedure:
    Stmt stmt = m.body;

    // Allocate a new frame pointer:
    FramePointer fp_ = fp.push();

    // Capture the return context as a continuation:
    Kont kont_ = new AssignKont(this.lhs, this.next, fp, kont);

    // Bind $this:
    ImmutableMap<Addr, Value> store_ = store;
    store_ = Store.extend(store_, fp_.offset("$this"), thiss);

    // Bind addresses to values of arguments:
    for (int i = 0; i < m.formals.length; ++i) {
      Addr a = fp_.offset(m.formals[i]);
      Value v = args[i].eval(fp, store);
      store_ = Store.extend(store_, a, v);
    }

    // Create the new state:
    return new State(stmt, fp_, store_, kont_);
  }

}

/**
 * A direct method call statement.
 */
final class InvokeStmt extends AbstractInvokeStmt {

  /**
   * The object with the method:
   */
  public final AExp object;

  /**
   * Creates a method invocation statement.
   */
  public InvokeStmt(Stmt next, String lhs, AExp object, String methodName, AExp[] args) {
    super(next, lhs, methodName, args);
    this.object = object;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Look up the object:
    ObjectValue thiss = (ObjectValue) object.eval(fp, store);

    // Check its class:
    ClassDef classs = ClassDef.forName(thiss.className);

    // Look up the method in this class:
    MethodDef method = classs.lookupMethod(methodName);

    // Apply the method:
    return applyMethod(method, thiss, fp, store, kont);
  }
}

/**
 * A statement to invoke the method in a parent class.
 */
final class InvokeSuperStmt extends AbstractInvokeStmt {

  /**
   * Creates super method invocation statement.
   */
  public InvokeSuperStmt(Stmt next, String lhs, String methodName, AExp[] args) {
    super(next, lhs, methodName, args);
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // First, get "this":
    ObjectValue thiss = (ObjectValue) store.get(fp.offset("$this"));

    // Find the parent of "this":
    ClassDef parent = ClassDef.forName(thiss.className).parentClass();

    // Look up the method in the parent:
    MethodDef method = parent.lookupMethod(methodName);

    // Apply the method:
    return applyMethod(method, thiss, fp, store, kont);
  }
}

/**
 * A statement which returns control to the caller.
 */
final class ReturnStmt extends Stmt {

  /**
   * An expression containing the result of the method.
   */
  public AExp result;

  /**
   * The result of the current procedure.
   */
  public ReturnStmt(Stmt next, AExp result) {
    super(next);
    this.result = result;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // Compute the return value:
    Value returnValue = result.eval(fp, store);

    // Apply the current continuation:
    return kont.apply(returnValue, store);
  }
}

/**
 * A print statement.
 */
final class PrintStmt extends Stmt {

  /**
   * Arguments to print
   */
  public AExp[] args;

  /**
   * The result of the current procedure.
   */
  public PrintStmt(Stmt next, AExp[] args) {
    super(next);
    this.args = args;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    // Print the arguments
    for (AExp object : args) {
      Value val = object.eval(fp, store);
      System.out.println(val.toPrint());
    }

    return new State(this.next, fp, store, kont);
  }
}

/**
 * A statement which assigns its result to a field.
 */
final class FieldAssignStmt extends Stmt {

  /**
   * The object with a field.
   */
  public final AExp object;

  /**
   * The field on the object.
   */
  public final String field;

  /**
   * The value to be assigned.
   */
  public final AExp rhs;

  /**
   * Creates a new field assignment statement.
   */
  public FieldAssignStmt(Stmt next, AExp object, String field, AExp rhs) {
    super(next);
    this.object = object;
    this.field = field;
    this.rhs = rhs;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Evaluate the object:
    Value obj = object.eval(fp, store);

    // Evaluate the right-hand side:
    Value val = rhs.eval(fp, store);

    // Compute the address of the field:
    Addr fieldAddr = obj.offset(field);

    // Bind the field address in the store:
    ImmutableMap<Addr, Value> store_ = Store.extend(store, fieldAddr, val);

    // Create the next state:
    return new State(next, fp, store_, kont);
  }
}

/**
 * A statement which pushes an exception-handling continuation on the stack.
 */
final class PushHandlerStmt extends Stmt {

  /**
   * The (super)type of exceptions to catch.
   */
  public String className;

  /**
   * The label to branch when an exception is caught.
   */
  public String label;

  /**
   * Creates a handler-pushing statement.
   */
  public PushHandlerStmt(Stmt next, String className, String label) {
    super(next);
    this.className = className;
    this.label = label;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Create a new continuation:
    Kont kont_ = new HandlerKont(className, label, kont);

    // Continuation to the next statement:
    return new State(next, fp, store, kont_);
  }
}

/**
 * A statement which pops an exception handler.
 */
final class PopHandlerStmt extends Stmt {

  public PopHandlerStmt(Stmt next) {
    super(next);
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Pop off the topmost handler:
    Kont kont_ = kont.popHandler();

    // Continue to the next statement:
    return new State(next, fp, store, kont_);
  }
}

/**
 * A statemente which throws an exception.
 */
final class ThrowStmt extends Stmt {

  /**
   * The exception to throw (which must evaluate to an object).
   */
  public final AExp exception;

  /**
   * Creates a throw statement.
   */
  public ThrowStmt(Stmt next, AExp exception) {
    super(next);
    this.exception = exception;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Evaluate the exception to be thrown:
    Value exceptionValue = exception.eval(fp, store);

    // Throw it at the stack:
    return kont.handle((ObjectValue) exceptionValue, fp, store);
  }
}

/**
 * A statement to move the last thrown exception into a register.
 * 
 * The last exception to be thrown is kept in the register $ex.
 */
final class MoveExceptionStmt extends Stmt {

  /**
   * The register to receive the exception value.
   */
  public final String register;

  /**
   * Creates a statement to capture the most recent exception.
   */
  public MoveExceptionStmt(Stmt next, String register) {
    super(next);
    this.register = register;
  }

  public State step(FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {

    // Capture the most recent exception from $ex:
    Value ex = store.get(fp.offset("$ex"));

    // Move the exception into the register:
    ImmutableMap<Addr, Value> store_ = Store.extend(store, fp.offset(register), ex);

    // Step to the next statement:
    return new State(next, fp, store_, kont);
  }

}

/* Expressions. */

/**
 * Atomic expressions are those which can be evaluated without an exception or
 * error, without side effects and without non-termination.
 */
abstract class AExp {

  /**
   * Returns the value of this expression with respect to the given frame
   * pointer and the current store.
   * 
   * @param fp
   *          the active frame pointer
   * @param store
   *          the current store
   * @return the result of the expression
   */
  abstract Value eval(FramePointer fp, ImmutableMap<Addr, Value> store);
}

/**
 * An expression that represents the current object.
 */
class ThisExp extends AExp {
  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    return store.get(fp.offset("$this"));
  }
}

/**
 * An expression to represent either true or false.
 */
class BooleanExp extends AExp {

  /**
   * The value of this boolean expression.
   */
  public final boolean value;

  public BooleanExp(boolean value) {
    this.value = value;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    if (value)
      return TrueValue.VALUE;
    else
      return FalseValue.VALUE;
  }
}

/**
 * An expression to represent the null value.
 */
class NullExp extends AExp {
  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    return NullValue.VALUE;
  }
}

/**
 * An expression to represent a lack of value.
 */
class VoidExp extends AExp {
  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    return VoidValue.VALUE;
  }
}

/**
 * An expression to represent a value held in a register.
 */
class RegisterExp extends AExp {

  /**
   * The name of the register.
   */
  public final String register;

  public RegisterExp(String register) {
    this.register = register;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    // Compute the address of the offset:
    Addr a = fp.offset(register);

    // Look up the address in the store:
    return store.get(a);
  }
}

/**
 * An expression representing a literal integer.
 */
class IntExp extends AExp {
  public final int value;

  public IntExp(int value) {
    this.value = value;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    return new IntValue(value);
  }
}

/**
 * Static constants for primitive operations.
 */
enum PrimOp {
  ADD,
  MUL,
  SUB,
  EQ
}

/**
 * An expression representing an atomic compuation.
 */
class AtomicOpExp extends AExp {

  /**
   * The operation.
   */
  public final PrimOp op;

  /**
   * The arguments to this operation.
   */
  public final AExp[] args;

  public AtomicOpExp(PrimOp op, AExp[] args) {
    this.op = op;
    this.args = args;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {

    // Dispatch on the type of the operation:
    switch (op) {

    case ADD:
      // Evaluate and sum all arguments:
      int sum = 0;
      for (int i = 0; i < args.length; ++i) {
        sum += args[i].eval(fp, store).toInt();
      }
      return new IntValue(sum);

    case MUL:
      // Evaluate and multiply all arguments:
      int prod = 1;
      for (int i = 0; i < args.length; ++i) {
        prod *= args[i].eval(fp, store).toInt();
      }
      return new IntValue(prod);

    case SUB:
      // Subtract: arg[0] - arg[1]
      int a = args[0].eval(fp, store).toInt();
      int b = args[1].eval(fp, store).toInt();
      return new IntValue(a - b);

    case EQ:
      // Check for equality: arg[0] == arg[1] ?
      int x = args[0].eval(fp, store).toInt();
      int y = args[1].eval(fp, store).toInt();
      return Value.from(x == y);

    default:
      throw new RuntimeException("unhandled atomic op: " + op);
    }
  }
}

/**
 * An expression which checks the (super)type of a value.
 */
class InstanceOfExp extends AExp {

  /**
   * The object whose type should be checked.
   */
  public final AExp object;

  /**
   * The (super)type to check for.
   */
  public final String className;

  public InstanceOfExp(AExp object, String className) {
    this.object = object;
    this.className = className;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {
    // Evaluate the object:
    ObjectValue obj = (ObjectValue) object.eval(fp, store);

    // The class of the object:
    ClassDef classs = ClassDef.forName(obj.className);

    // Create a boolean with the result:
    return Value.from(classs.isInstanceOf(className));
  }
}

/**
 * An expression to retrieve a field from an object.
 * 
 * An object is a base address -- an object pointer -- the addresses of fields
 * are computed as abstract "offsets" from this base pointer.
 */
class FieldExp extends AExp {

  /**
   * The object with the field.
   */
  public final AExp object;

  /**
   * The name of the field.
   */
  public final String field;

  public FieldExp(AExp object, String field) {
    this.object = object;
    this.field = field;
  }

  Value eval(FramePointer fp, ImmutableMap<Addr, Value> store) {

    // Evaluate the object:
    ObjectValue op = (ObjectValue) object.eval(fp, store);

    // Compute the address of the field:
    Addr fieldAddr = op.offset(field);

    return store.get(fieldAddr);
  }
}

/* Semantic domains. */

/**
 * A pointer is an abstract location in memory.
 */
abstract class Pointer {

  /**
   * The internal value of the pointer.
   */
  final long value;

  protected Pointer() {
    this.value = ++maxPointer;
  }

  private static long maxPointer = 0;

  /**
   * An ordering on pointers.
   */
  public static final Comparator<Pointer> ordering = new Comparator<Pointer>() {
    public int compare(Pointer p1, Pointer p2) {
      if (p1.value < p2.value)
        return -1;
      else if (p2.value < p1.value)
        return 1;
      else
        return 0;
    }
  };

  /**
   * Calculates an offset address from this pointer.
   */
  public abstract Addr offset(String name);
}

/**
 * A frame pointer is a pointer into the stack; local variables live as abstract
 * offsets from these pointers.
 */
class FramePointer extends Pointer {
  public FramePointer() {
    super();
  }

  /**
   * Allocates a new frame pointer, as in when a new procedure is called.
   */
  public FramePointer push() {
    return new FramePointer();
  }

  /**
   * Computes an abstract offset from this pointer, given the name of a register
   * (a local variable).
   */
  public Addr offset(String register) {
    return new FrameAddr(this, register);
  }
}

/**
 * An object pointer is a pointer into the heap; fields live as abstract offsets
 * from these pointers.
 */
class ObjectPointer extends Pointer {

  public ObjectPointer() {
    super();
  }

  /**
   * Computes an abstract offset from this pointer, given the name of the field.
   */
  public Addr offset(String fieldName) {
    return new FieldAddr(this, fieldName);
  }

}

/**
 * An offset address is a pairing of a pointer and an abstract offset (a
 * string).
 */
abstract class OffsetAddr extends Addr {

  /**
   * The base pointer.
   */
  public final Pointer pointer;

  /**
   * The abstract offset.
   */
  public final String offset;

  public OffsetAddr(Pointer pointer, String offset) {
    this.pointer = pointer;
    this.offset = offset;
  }

  /**
   * An ordering on offset addresses.
   */
  public static final Comparator<OffsetAddr> ordering = new Comparator<OffsetAddr>() {
    public int compare(OffsetAddr oa1, OffsetAddr oa2) {
      if (oa1.pointer.value < oa2.pointer.value)
        return -1;
      if (oa2.pointer.value > oa1.pointer.value)
        return 1;
      return oa1.offset.compareTo(oa2.offset);
    }
  };
}

/**
 * A frame address is an offset address in the stack.
 */
class FrameAddr extends OffsetAddr {
  public FrameAddr(FramePointer fp, String offset) {
    super(fp, offset);
  }
}

/**
 * A field address is an offset address in the heap.
 */
class FieldAddr extends OffsetAddr {
  public FieldAddr(ObjectPointer op, String field) {
    super(op, field);
  }
}

/**
 * An address maps to a value in the store.
 */
abstract class Addr {

  /**
   * An ordering on addresses.
   */
  public static final Comparator<Addr> ordering = new Comparator<Addr>() {
    public int compare(Addr a1, Addr a2) {
      // Use reflection to compare on class names first:
      int cmp = a1.getClass().toString().compareTo(a2.getClass().toString());

      if (cmp != 0)
        return cmp;

      if (a1 instanceof OffsetAddr)
        return OffsetAddr.ordering.compare((OffsetAddr) a1, (OffsetAddr) a2);

      throw new RuntimeException("Cannot compare!");
    }
  };
}

/**
 * A run-time value.
 */
abstract class Value {

  /**
   * Assuming the value is an object pointer, compute an offset.
   */
  public Addr offset(String fieldName) {
    throw new RuntimeException("cannot offset non-object pointer: " + this);
  }

  /**
   * Convert the value to a Boolean.
   */
  public Boolean toBoolean() {
    return this != FalseValue.VALUE;
  }

  /**
   * Assuming the value is an integer, extract it.
   */
  public int toInt() {
    throw new RuntimeException("cannot convert to int: " + this);
  }

  /**
   * Given a Boolean, returns an encapsulating value.
   */
  public static final Value from(boolean value) {
    if (value)
      return TrueValue.VALUE;
    else
      return FalseValue.VALUE;
  }
  
  public abstract String toPrint();
}

/**
 * A singleton class for the true value.
 */
class TrueValue extends Value {

  private TrueValue() {}

  public static final TrueValue VALUE = new TrueValue();

  @Override
  public String toPrint() {
    return "true";
  }
}

/**
 * A singleton class for the false value.
 */
class FalseValue extends Value {

  private FalseValue() {}

  public static final FalseValue VALUE = new FalseValue();
  
  @Override
  public String toPrint() {
    return "false";
  }
}

/**
 * A singleton class for the null value.
 */
class NullValue extends Value {

  private NullValue() {}

  public static final NullValue VALUE = new NullValue();
  
  @Override
  public String toPrint() {
    return "null";
  }
}

/**
 * A singleton class for the void value.
 */
class VoidValue extends Value {

  private VoidValue() {}

  public static final VoidValue VALUE = new VoidValue();
  
  @Override
  public String toPrint() {
    return "void";
  }
}

/**
 * Integer values are encapsulated in their own class.
 */
class IntValue extends Value {

  /**
   * The value of this integer.
   */
  public final int value;

  public IntValue(int value) {
    this.value = value;
  }

  public int toInt() {
    return value;
  }
  
  @Override
  public String toPrint() {
    return String.valueOf(this.value);
  }

}

/**
 * An object value pairs an object pointer with the class name.
 */
class ObjectValue extends Value {

  /**
   * The base pointer for the object.
   */
  public final ObjectPointer pointer;

  /**
   * The class name for the object's type.
   */
  public final String className;

  public ObjectValue(String className, ObjectPointer pointer) {
    this.className = className;
    this.pointer = pointer;
  }

  /**
   * Checks whether this object is an instance of a class.
   */
  public boolean isInstanceOf(String otherClassName) {
    return ClassDef.forName(className).isInstanceOf(otherClassName);
  }

  /**
   * Properly computes an offset from the object pointer.
   */
  public Addr offset(String fieldName) {
    return pointer.offset(fieldName);
  }
  
  @Override
  public String toPrint() {
    return "TODO";
  }
}

/**
 * The store is a map from addresses to value.
 * 
 * The store in this machine is an ImmutableMap<Addr,Value>.
 * 
 * This class contains utility methods.
 */
class Store {
  public static final ImmutableMap<Addr, Value> extend(ImmutableMap<Addr, Value> store, Addr addr,
      Value value) {
    return new ImmutableMap.Builder<Addr, Value>().putAll(store).put(addr, value).build();
  }
}

/* Continuations. */

/**
 * A continuation is a representation of the program stack.
 * 
 * Every continuation except the halt continuation will have a continuation
 * beneath it.
 */
abstract class Kont {

  /**
   * The continuation beneath this one.
   */
  public final Kont next;

  public Kont(Kont next) {
    this.next = next;
  }

  /**
   * Applies this continuation to a return value.
   * 
   * This returns execution to the context in which the continuation was
   * created. It is effectively procedure return.
   * 
   * Any exception handlers in the way of the next return point are popped off.
   */
  public abstract State apply(Value returnValue, ImmutableMap<Addr, Value> store);

  /**
   * Assuming the top of the stack is a handler, pop it off and return the next
   * continuation.
   */
  public Kont popHandler() {
    throw new RuntimeException("no handler to pop!");
  }

  /**
   * Invokes the top-most exception handler.
   * 
   * Any return points in the way are popped off.
   * 
   * It continues down the stack until it finds a handler's type that matches
   * the exception.
   */
  public abstract State handle(ObjectValue exception, FramePointer fp,
      ImmutableMap<Addr, Value> store);
}

/**
 * A handler continuation is essentially a stack frame that contains the type of
 * exception it catches and a label to which to branch upon a match.
 */
class HandlerKont extends Kont {

  /**
   * The class name for the exception.
   */
  public final String className;

  /**
   * The label to which the jump after catching the exception.
   */
  public final String label;

  public HandlerKont(String className, String label, Kont kont) {
    super(kont);

    this.className = className;
    this.label = label;
  }

  /**
   * Continuation handlers can't be applied for procedure return, so they go to
   * the next one.
   */
  public State apply(Value returnValue, ImmutableMap<Addr, Value> store) {
    return next.apply(returnValue, store);
  }

  /**
   * Returns the continuation underneath this handler.
   */
  public Kont popHandler() {
    return next;
  }

  public State handle(ObjectValue exception, FramePointer fp, ImmutableMap<Addr, Value> store) {
    if (exception.isInstanceOf(className)) {
      // Place the exception at (fp,"$ex")
      ImmutableMap<Addr, Value> store_ = Store.extend(store, fp.offset("$ex"), exception);
      return new State(Stmt.forLabel(label), fp, store_, next);
    }

    else
      return next.handle(exception, fp, store);
  }
}

/**
 * An assignment continuation awaits a return value to be assigned to register.
 */
class AssignKont extends Kont {

  /**
   * The register awaiting the result.
   */
  public final String register;

  /**
   * The statement at which to resume.
   */
  public final Stmt stmt;

  /**
   * The frame pointer to restore.
   */
  public final FramePointer fp;

  public AssignKont(String register, Stmt stmt, FramePointer fp, Kont kont) {
    super(kont);
    this.register = register;
    this.stmt = stmt;
    this.fp = fp;
  }

  /**
   * Performs the impending assignment and restores the context.
   */
  public State apply(Value returnValue, ImmutableMap<Addr, Value> store) {

    // Place the result in the register:
    ImmutableMap<Addr, Value> store_ = Store.extend(store, fp.offset(register), returnValue);

    // Restore the old context:
    return new State(stmt, fp, store_, next);
  }

  /**
   * Skips down the stack to the next handler.
   */
  public State handle(ObjectValue exception, FramePointer fp, ImmutableMap<Addr, Value> store) {
    // Pick up the current pointer:
    return next.handle(exception, this.fp, store);
  }
}

/**
 * A halt continuation signals the end of the computation.
 */
class HaltKont extends Kont {
  private HaltKont() {
    super(null);
  }

  /**
   * Terminates the computation with an exception.
   */
  public State apply(Value returnValue, ImmutableMap<Addr, Value> store) {
    throw new RuntimeException("terminated: " + returnValue);
  }

  public State handle(ObjectValue exception, FramePointer fp, ImmutableMap<Addr, Value> store) {
    throw new RuntimeException("uncaught exception: " + exception);
  }

  public static final HaltKont HALT = new HaltKont();
}

/**
 * A state of execution in the OO-CESK machine.
 */
class State {

  /**
   * The "C" component: the current statement is the control string.
   */
  public final Stmt stmt;

  /**
   * The "E" component: environments in this machine are flat: the address of
   * all ocal variables (registers) are computed as offsets from the frame
   * pointer.
   */
  public final FramePointer fp;

  /**
   * The "S" component: stores in this machine map address to values.
   */
  public final ImmutableMap<Addr, Value> store;

  /**
   * The "K" component: a stack of exception handlers and return points.
   */
  public final Kont kont;

  public State(Stmt stmt, FramePointer fp, ImmutableMap<Addr, Value> store, Kont kont) {
    this.stmt = stmt;
    this.fp = fp;
    this.store = store;
    this.kont = kont;
  }

  /**
   * Returns the next state of execution.
   * 
   * @return the next state
   */
  public State next() {
    return stmt == null ? null : stmt.step(fp, store, kont);
  }
}

class OOCESK {

  /**
   * Executes the main method in the supplied class.
   * 
   * @param mainClass
   *          the class with a main method
   */
  public static void execute(ClassDef mainClass) {
    // Grab the main method:
    MethodDef mainMethod = mainClass.lookupMethod("main");

    // Construct an object pointer for mainClass:
    ObjectPointer op = new ObjectPointer();

    // Construct an object value for mainClass:
    ObjectValue obj = new ObjectValue(mainClass.name, op);

    // Allocate an initial frame pointer:
    FramePointer fp0 = new FramePointer();

    // Create an initial store:
    ImmutableMap<Addr, Value> store0 =
      new ImmutableSortedMap.Builder<Addr, Value>(Addr.ordering).build();

    // Insert the initial object at register $this:
    store0 = Store.extend(store0, fp0.offset("this"), obj);

    // Grab the halt continuation:
    Kont halt = HaltKont.HALT;

    // Synthesize the initial state:
    State state = new State(mainMethod.body, fp0, store0, halt);

    // Run until termination:
    while (state != null) {
      state = state.next();
    }
  }

  public static void main(String[] args) {
    // If you want to run this interpreter,
    // you should parse the classes into
    // a collection of ClassDef's.

    // Then call execute() on the class
    // that contains the main method.
  }
}
