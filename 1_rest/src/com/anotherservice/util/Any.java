package com.anotherservice.util;

import java.util.*;

/**
 * This class is a Map and a List typically for Json data.
 *
 * @author  Simon Uyttendaele
 */
public class Any implements Iterable<Any>, Jsonizable, Dejsonizable
{
	public static Any wrap(Object o)
	{
		if( o instanceof Any )
			return (Any)o;
		//if( o == null )
		//	return null;
		if( o instanceof List )
		{
			if( ((List)o).size() > 0 && ((List)o).get(0) instanceof Any )
				return new Any(o);
			else
			{
				Any a = new Any(null);
				List<Any> l = a.list();
				for( Object v : (List)o )
					a.add(wrap(v));
				return a;
			}
		}
		else if( o instanceof Map )
		{
			Iterator<Map.Entry> i = ((Map)o).entrySet().iterator();
			Map.Entry me = (i.hasNext() ? i.next() : null);
			
			if( me != null && me.getKey() instanceof String && me.getValue() instanceof Any )
				return new Any(o);
			else
			{
				Any a = new Any(null);
				Map<String, Any> m = a.map();
				for( Map.Entry e : ((Map<?,?>)o).entrySet() )
					a.put("" + e.getKey(), wrap(e.getValue()));
				return a;
			}
		}
		else 
			return new Any(o);
	}
	
	public static Any empty() { return wrap(null); }
	
	private Object _value = null;
	
	private Any(Object o)
	{
		_value = o;
	}
	
	public List<Any> list()
	{
		if( _value instanceof List )
			return (List<Any>)_value;
		else if( _value == null )
			return (List<Any>)(_value = new LinkedList<Any>());
		else
			return null;
	}
	
	public boolean isList() { return (_value instanceof List); }
	public boolean isList(String key) { Any a = get(key); return a != null && a.isList(); }
	public boolean isList(int index) { Any a = get(index); return a != null && a.isList(); }
	
	public Map<String, Any> map()
	{
		if( _value instanceof Map )
			return (Map<String, Any>)_value;
		else if( _value == null )
			return (Map<String, Any>)(_value = new HashMap<String, Any>());
		else
			return null;
	}
	
	public boolean isMap() { return (_value instanceof Map); }
	public boolean isMap(String key) { Any a = get(key); return a != null && a.isMap(); }
	public boolean isMap(int index) { Any a = get(index); return a != null && a.isMap(); }
	
	public boolean isNull() { return _value == null; }
	public boolean isNull(String key) { Any a = get(key); return a == null || a.isNull(); }
	public boolean isNull(int index) { Any a = get(index); return a == null || a.isNull(); }
	
	public boolean exists(int index) { try { Any a = get(index); return a != null; } catch(Exception e) { return false; } }
	public boolean exists(String key) { try { Any a = get(key); return a != null; } catch(Exception e) { return false; } }
	
	public <T> T value(String key)
	{
		Object v = get(key);

		if( v == null )
			return null;
		if( v instanceof Any )
			return ((Any)v).<T>value();

		try { return (T) v; }
		catch(ClassCastException icce) { return null; }
	}
	
	public <T> T value(int index)
	{
		Object v = get(index);
		if( v == null )
			return null;
		if( v instanceof Any )
			return ((Any)v).<T>value();

		try { return (T) v; }
		catch(ClassCastException icce) { return null; }
	}
	
	public <T> T value()
	{
		if( _value == null )
			return null;
		try { return (T) _value; }
		catch(ClassCastException icce) { return null; }
	}
	
	public Object unwrap()
	{
		if( isList() )
		{
			List<Object> l = new LinkedList<Object>();
			for( Any a : this )
				l.add(a.unwrap());
			return l;
		}
		
		if( isMap() )
		{
			Map<String, Object> m = new HashMap<String, Object>();
			for( String k : keys() )
			{
				Any a = get(k);
				m.put(k, (a == null ? null : a.unwrap()));
			}
			return m;
		}
		
		if( _value instanceof Any )
			return ((Any)_value).unwrap();

		return _value;
	}
	public Object unwrap(String key) { if( isNull(key) ) return null; return get(key).unwrap(); }
	public Object unwrap(int index) { if( isNull(index) ) return null; return get(index).unwrap(); }
	
	// =====================================
	// List + Map
	// =====================================
	
	public void clear()
	{
		if( isMap() )
			map().clear();
		else if( isList() )
			list().clear();
	}
	
	public boolean equals(Object o)
	{
		boolean isAnyNull = o instanceof Any && ((Any) o).isNull();
		if( (o == null || isAnyNull) && _value == null )
			return true;
		else if( o == null || _value == null )
			return false;
		else if( o == this )
			return true;
		else if( o instanceof Any )
		{
			Any a = (Any)o;
			if( isList() )
			{
				if( !a.isList() || a.size() != size() ) 
					return false;
				for( Any b : this )
					if( !a.contains(b) )
						return false;
				return true;
			}
			else if( isMap() )
			{
				if( !a.isMap() || a.size() != size() ) 
					return false;
				for( String key : keys() )
					if( !a.containsKey(key) || !get(key).equals(a.get(key)) )
						return false;
				return true;
			}
			else
				return o.equals(_value);
		}
		else
			return _value.equals(o);
	}

	public int hashCode()
	{
		return super.hashCode();
	}

	public boolean isEmpty()
	{
		if( isMap() )
			return map().isEmpty();
		else if( isList() )
			return list().isEmpty();
		else
			return _value == null;
	}
	public boolean isEmpty(String key) { Any a = get(key); return a == null || a.isEmpty(); }
	public boolean isEmpty(int index) { Any a = get(index); return a == null || a.isEmpty(); }
	
	public int size()
	{
		if( isMap() )
			return map().size();
		else if( isList() )
			return list().size();
		else
			return 0;
	}
	
	public Any pipe(String key, Object value) { add(key, value); return this; }
	public Any pipe(int index, Object value) { add(index, value); return this; }
	
	// =====================================
	// List
	// =====================================
	
	public boolean add(Object e)
	{
		return list().add(wrap(e));
	}
	
	public void add(String key, Object element) { put(key, element); }
	public void add(int index, Object element)
	{
		if( isList() || _value == null )
			list().add(index, wrap(element));
		else if( isMap() )
			map().put("" + index, wrap(element));
		else
			throw new ArrayIndexOutOfBoundsException();
	}
	
	public boolean addAll(Any a)
	{
		if( a == null ) return false;
		if( (_value == null || isList()) && a.isList() )
			list().addAll(a.list());
		else if( (_value == null || isMap()) && a.isMap() )
			map().putAll(a.map());
		else if( isList() && a.isMap() )
			list().addAll(a.values());
		else if( isMap() && a.isList() )
		{
			List<Any> l = a.list();
			Map<String, Any> m = map();
			for( int i = 0; i < l.size(); i++ )
				m.put("" + i, l.get(i));
		}
		
		return true;
	}
	
	public boolean addAll(Collection<?> c)
	{
		if( c != null )
			for( Object v : c )
				return list().add(wrap(c));
		return true;
	}
	
	public boolean addAll(int index, Collection<?> c)
	{
		if( c != null )
			for( Object v : c )
				list().add(index++, wrap(c));
		return true;
	}
	
	public boolean contains(Object o)
	{
		if( isMap() )
			return map().containsValue(o);
		else if( isList() )
			return list().contains(o);
		else
			return false;
	}
	
	public boolean containsAll(Collection<?> c)
	{
		if( c == null ) return false;
		if( isList() )
			return list().containsAll(c);
		else if( isMap() )
		{
			Map<String, Any> m = map();
			for( Object a : c )
				if( !m.containsValue(a) )
					return false;
			return true;
		}
		else 
			return false;
	}
	
	public Any get(int index)
	{
		if( isList() )
			return list().get(index);
		else if( isMap() )
			return map().get("" + index);
		else
			throw new ArrayIndexOutOfBoundsException();
	}
	
	public int indexOf(Object o)
	{
		if( isList() )
			return list().indexOf(o);
		else
			return -1;
	}
	
	public Iterator<Any> iterator()
	{
		if( isList() )
			return list().iterator();
		else if( isMap() )
			return map().values().iterator();
		else
			return Collections.emptyIterator();
	}
	
	public int lastIndexOf(Object o)
	{
		return list().lastIndexOf(o);
	}
	
	public ListIterator<Any> listIterator()
	{
		return list().listIterator();
	}
	
	public ListIterator<Any> listIterator(int index)
	{
		return list().listIterator(index);
	}
	
	public boolean removeAll(Collection<?> c)
	{
		if( c == null ) return false;
		if( isList() )
			return list().removeAll(c);
		else if( isMap() )
			return map().values().removeAll(c);
		else
			return false;
	}
	
	public boolean retainAll(Collection<?> c)
	{
		if( c == null ) return false;
		if( isList() )
			return list().retainAll(c);
		else if( isMap() )
			return map().values().retainAll(c);
		else
			return false;
	}
	
	public void set(String key, Object element) { put(key, element); }
	public Any set(int index, Object element)
	{
		if( isList() || _value == null )
			return list().set(index, wrap(element));
		else if( isMap() )
			return map().put("" + index, wrap(element));
		else
			throw new ArrayIndexOutOfBoundsException();
	}
	
	public List<Any> subList(int fromIndex, int toIndex)
	{
		if( !isList() )
			return Collections.EMPTY_LIST;
		return list().subList(fromIndex, toIndex);
	}
	
	public Any[] toArray()
	{
		if( isList() )
			return (Any[])list().toArray();
		else if( isMap() )
			return (Any[])map().values().toArray();
		else
			return new Any[] { };
	}
	
	public <T extends Any> T[] toArray(T[] a)
	{
		if( isList() )
			return list().toArray(a);
		else if( isMap() )
			return map().values().toArray(a);
		else
			return null;
	}
	
	// =====================================
	// Map
	// =====================================
	
	public boolean containsKey(Object key)
	{
		if( key == null ) return false;
		if( key instanceof Any && ((Any)key)._value instanceof String )
			key = ((Any)key).<String>value();
		if( key instanceof Any && ((Any)key)._value instanceof Integer )
			key = ((Any)key).<Integer>value();

		if( isMap() )
			return map().containsKey("" + key);
		else if( isList() && key instanceof Integer )
			return list().size() > (Integer) key;
		else
			return false;
	}
	
	public boolean containsValue(Object value)
	{
		if( isList() )
			return list().contains(value);
		else if( isMap() )
			return map().containsValue(value);
		else
			return equals(value);
	}
	
	public Set<Map.Entry<String, Any>> entrySet()
	{
		if( !isMap() )
			return Collections.EMPTY_SET;
		return map().entrySet();
	}
	
	public Any get(Object key)
	{
		if( key == null )
			return null;
		
		if( key instanceof Any && ((Any)key)._value instanceof String )
			key = ((Any)key).<String>value();
		if( key instanceof Any && ((Any)key)._value instanceof Integer )
			key = ((Any)key).<Integer>value();
			
		if( isMap() )
			return map().get("" + key);
		else if( isList() && key instanceof Integer )
			return list().get((Integer) key);
		else
			return null;
	}
	
	public Set<String> keys() { return keySet(); }
	
	public Set<String> keySet()
	{
		if( !isMap() )
			return Collections.EMPTY_SET;
		return map().keySet();
	}
	
	public void put(int index, Object element) { add(index, element); }
	public Any put(Any key, Object value)
	{
		if( key == null )
			return null;
		
		String ks = null;
		Integer ki = null;
		if( key._value instanceof String )
			ks = key.<String>value();
		else if( key._value instanceof Integer )
		{
			ki = key.<Integer>value();
			ks = "" + ki;
		}
		else
			return null;
		
		if( isMap() )
			return map().put(ks, wrap(value));
		else if( isList() )
			return list().set(ki, wrap(value));
		else
			return null;
	}
	
	public Any put(String key, Object value)
	{
		return map().put(key, wrap(value));
	}
	
	public Any put(String key, Any value)
	{
		return map().put(key, value);
	}
	
	public void putAll(Any a)
	{
		if( a == null ) return;
		if( (_value == null || isList()) && a.isList() )
			list().addAll(a.list());
		else if( (_value == null || isMap()) && a.isMap() )
			map().putAll(a.map());
		else if( isList() && a.isMap() )
			list().addAll(a.values());
		else if( isMap() && a.isList() )
		{
			List<Any> l = a.list();
			Map<String, Any> m = map();
			for( int i = 0; i < l.size(); i++ )
				m.put("" + i, l.get(i));
		}
	}
	
	public void putAll(Map<?, ?> m)
	{
		if( m == null ) return;
		Map<String, Any> mm = map();
		for( Object key : m.keySet() )
			mm.put("" + key, wrap(m.get(key)));
	}
	
	public Any remove(Object key)
	{
		if( key == null ) return null;
		if( key instanceof Any && ((Any)key)._value instanceof String )
			key = ((Any)key).<String>value();
		if( key instanceof Any && ((Any)key)._value instanceof Integer )
			key = ((Any)key).<Integer>value();

		if( isMap() && key instanceof String )
			return map().remove(key);
		else if( isList() && key instanceof Integer )
			return list().remove((int) key);
		else if( isMap() )
		{
			Collection<Any> c = map().values();
			for( Any a : c )
			{
				if( a.equals(key) )
				{
					c.remove(key);
					return a;
				}
			}
			return null;
		}
		else if( isList() )
		{
			int index = list().indexOf(key);
			if( index >= 0 )
				return list().remove(index);
			else
				return null;
		}
		else
			return null;
	}
	
	public Collection<Any> values()
	{
		if( isMap() )
			return map().values();
		else if( isList() )
			return list();
		else
			return Collections.EMPTY_LIST;
	}
	
	// =====================================
	// (De)Jsonizable
	// =====================================
	
	public String toJson()
	{
		StringBuilder b = new StringBuilder();
		if( isList() )
		{
			b.append('[');
			for( Any a : this )
			{
				b.append(Json.encode(a));
				b.append(',');
			}
			if( b.charAt(b.length() - 1) == ',' )
				b.setCharAt(b.length() - 1, ']');
			else
				b.append(']');
		}
		else if( isMap() )
		{
			b.append('{');
			for( Map.Entry<String, Any> e : this.entrySet() )
			{
				b.append(Json.encode(e.getKey()));
				b.append(':');
				b.append(Json.encode(e.getValue()));
				b.append(',');
			}
			if( b.charAt(b.length() - 1) == ',' )
				b.setCharAt(b.length() - 1, '}');
			else
				b.append('}');
		}
		else
		{
			b.append(Json.encode(_value));
		}
		
		return b.toString();
	}
	
	public void fromJson(Object o)
	{
		Any a = wrap(o);
		_value = a._value;
	}
	
	public String toString() { return "" + _value; }
}