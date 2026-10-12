@file:Suppress("unused", "UNCHECKED_CAST")
package cn.fuckhome.xiaowine.utils
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.*

typealias Hooker = (MethodHookParam) -> Unit
class MethodHookParam(chain: XposedInterface.Chain) {
    private val chain = chain
    val method = chain.executable
    val thisObject: Any? = chain.thisObject
    var args: Array<Any?> = chain.args.toTypedArray()
    internal var early = false
    private var value: Any? = null
    private var failure: Throwable? = null
    var result: Any?
        get() = value
        set(v) { value = v; failure = null; early = true }
    var throwable: Throwable?
        get() = failure
        set(v) { failure = v; value = null; early = true }
    internal fun proceed() = chain.proceed(args)
    internal fun record(v: Any?, t: Throwable?) { value = v; failure = t }
    internal fun finish(): Any? { failure?.let { throw it }; return value }
}
private fun report(t: Throwable) { runCatching { android.util.Log.e("FuckHome", "Hook callback failed", t) } }
internal fun executeBefore(chain: XposedInterface.Chain, hooker: Hooker): Any? {
    val p = MethodHookParam(chain)
    val saved = p.args.copyOf()
    try { hooker(p) } catch(t: Throwable) { report(t); p.args = saved; return p.proceed() }
    return if(p.early) p.finish() else p.proceed()
}
internal fun executeAfter(chain: XposedInterface.Chain, hooker: Hooker): Any? {
    val p = MethodHookParam(chain)
    try { p.record(p.proceed(), null) } catch(t: Throwable) { p.record(null, t) }
    val result = p.result; val failure = p.throwable
    try { hooker(p) } catch(t: Throwable) { report(t); p.record(result, failure) }
    return p.finish()
}
fun Executable.hookBefore(h: Hooker) = ModernXposed.module.hook(this).setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH).intercept { executeBefore(it,h) }
fun Executable.hookAfter(h: Hooker) = ModernXposed.module.hook(this).setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH).intercept { executeAfter(it,h) }
fun Executable.hookBeforeMethod(h: Hooker) = hookBefore(h)
fun Executable.hookAfterMethod(h: Hooker) = hookAfter(h)
fun Executable.replaceMethod(h: (MethodHookParam)->Any?) = hookBefore { it.result=h(it) }
fun Executable.hookReturnConstant(v: Any?) = replaceMethod { v }
fun Iterable<Executable>.hookReturnConstant(v: Any?) = map { it.hookReturnConstant(v) }
fun Iterable<Executable>.hookBefore(h: Hooker) = map { it.hookBefore(h) }
fun Iterable<Executable>.hookAfter(h: Hooker) = map { it.hookAfter(h) }
fun String.findClass(loader: ClassLoader = ModernXposed.classLoader): Class<*> = Class.forName(this,false,loader)
private fun Class<*>.hierarchy(): Sequence<Class<*>> = generateSequence(this) { it.superclass }
fun findMethod(name: String, p: Method.()->Boolean): Method = name.findClass().hierarchy().flatMap { it.declaredMethods.asSequence() }.first { it.p() }.apply { setAccessible(true) }
fun findAllMethods(name: String, p: Method.()->Boolean): List<Method> = name.findClass().declaredMethods.filter { it.p() }.onEach { setAccessible(true) }
fun findConstructor(name: String, p: Constructor<*>.()->Boolean): Constructor<*> = name.findClass().declaredConstructors.first { it.p() }.apply { setAccessible(true) }
private fun types(args: Array<out Any?>): Array<Class<*>> = args.map { if(it is Class<*>) it else (it as String).findClass() }.toTypedArray()
private fun Class<*>.exact(name: String?, args: Array<out Any?>): Method = hierarchy().mapNotNull { runCatching { it.getDeclaredMethod(requireNotNull(name),*types(args)) }.getOrNull() }.first().apply { setAccessible(true) }
fun Class<*>.hookBeforeMethod(name: String?, vararg args: Any?, hooker: Hooker) = exact(name,args).hookBefore(hooker)
fun Class<*>.hookAfterMethod(name: String?, vararg args: Any?, hooker: Hooker) = exact(name,args).hookAfter(hooker)
fun Class<*>.replaceMethod(name: String?, vararg args: Any?, replacer: (MethodHookParam)->Any?) = exact(name,args).replaceMethod(replacer)
fun Class<*>.hookBeforeAllMethods(name: String?, h: Hooker) = declaredMethods.filter { it.name==name }.map { it.hookBefore(h) }
fun Class<*>.hookAfterAllMethods(name: String?, h: Hooker) = declaredMethods.filter { it.name==name }.map { it.hookAfter(h) }
fun Class<*>.findField(name: String?): Field = hierarchy().mapNotNull { runCatching { it.getDeclaredField(requireNotNull(name)) }.getOrNull() }.first().apply { setAccessible(true) }
fun Any?.getObjectField(name: String?): Any? = requireNotNull(this).javaClass.findField(name).get(this)
fun <T> Any?.getObjectAs(name: String): T = getObjectField(name) as T
fun <T> T.setObjectField(name: String?, value: Any?): T = apply { requireNotNull(this).javaClass.findField(name).set(this,value) }
fun <T> T.putObject(name: String, value: Any?): T = setObjectField(name,value)
fun Any?.getIntField(name: String?) = (getObjectField(name) as Number).toInt()
fun <T> T.setIntField(name: String?, value: Int): T = setObjectField(name,value)
fun <T> T.setBooleanField(name: String?, value: Boolean): T = setObjectField(name,value)
fun Class<*>.getStaticObjectField(name: String?): Any? = findField(name).get(null)
fun Class<*>.setStaticObjectField(name: String?, value: Any?) = apply { findField(name).set(null,value) }
private fun Class<*>.boxed(): Class<*> = when(this) {
    Boolean::class.javaPrimitiveType -> java.lang.Boolean::class.java
    Int::class.javaPrimitiveType -> java.lang.Integer::class.java
    Long::class.javaPrimitiveType -> java.lang.Long::class.java
    Float::class.javaPrimitiveType -> java.lang.Float::class.java
    Double::class.javaPrimitiveType -> java.lang.Double::class.java
    Byte::class.javaPrimitiveType -> java.lang.Byte::class.java
    Short::class.javaPrimitiveType -> java.lang.Short::class.java
    Char::class.javaPrimitiveType -> java.lang.Character::class.java
    else -> this
}
private fun <T: Executable> select(candidates: List<T>, args: Array<out Any?>): T {
    val matches = candidates.filter { e -> e.parameterCount==args.size && e.parameterTypes.indices.all { i -> val a=args[i]; val t=e.parameterTypes[i]; if(a==null) !t.isPrimitive else t.boxed().isAssignableFrom(a.javaClass) } }.distinctBy { it.parameterTypes.toList() }
    val best=matches.filter { a -> matches.none { b -> b!==a && a.parameterTypes.indices.all { i -> a.parameterTypes[i].boxed().isAssignableFrom(b.parameterTypes[i].boxed()) } && a.parameterTypes.indices.any { i -> a.parameterTypes[i].boxed()!=b.parameterTypes[i].boxed() } } }
    require(best.size==1) { "No unique matching executable: $candidates" }
    return best.single().apply { setAccessible(true) }
}
private fun Method.invokeUnwrapped(receiver: Any?, args: Array<out Any?>): Any? = try { invoke(receiver,*args) } catch(e: InvocationTargetException) { throw e.targetException }
fun Any?.callMethod(name: String?, vararg args: Any?): Any? { val r=requireNotNull(this); return select(r.javaClass.hierarchy().flatMap { it.declaredMethods.asSequence() }.filter { it.name==name && !Modifier.isStatic(it.modifiers) }.toList(),args).invokeUnwrapped(r,args) }
fun Class<*>.callStaticMethod(name: String?, vararg args: Any?): Any? = select(hierarchy().flatMap { it.declaredMethods.asSequence() }.filter { it.name==name && Modifier.isStatic(it.modifiers) }.toList(),args).invokeUnwrapped(null,args)
fun Class<*>.new(vararg args: Any?): Any = try { select(declaredConstructors.toList(),args).newInstance(*args) } catch(e: InvocationTargetException) { throw e.targetException }