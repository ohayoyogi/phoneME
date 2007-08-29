/*
 *   
 *
 * Copyright  1990-2007 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 */

# include "incls/_precompiled.incl"
# include "incls/_EventLogger.cpp.incl"

#if USE_EVENT_LOGGER

const char* const EventLogger::_event_names[] = {
# define NAME_EVENT_LOGGER_TYPE(x) #x,
  EVENT_LOGGER_TYPES_DO(NAME_EVENT_LOGGER_TYPE)
# undef NAME_EVENT_LOGGER_TYPE
};


jlong EventLogger::Entry::_last;
jlong EventLogger::Entry::_freq;
bool  EventLogger::Entry::_use_usec;

inline jlong EventLogger::Entry::now( void ) {
  return Os::elapsed_counter();
}

inline void EventLogger::Entry::initialize( void ) {
  _last = now();

  const jlong freq = Os::elapsed_frequency();
  _freq = freq;
  _use_usec = freq > 100 * 1000;
}

inline void EventLogger::Entry::set ( const EventType type, const jlong time ) {
  const unsigned delta = unsigned(time - _last);
  GUARANTEE( (delta >> delta_bits) == 0, "delta overflow" );
  _packed_data = (type << delta_bits) | delta;
  _last = time;
}

inline jlong
EventLogger::Entry::dump( Stream* s, jlong time ) const {
  time += delta();
  jlong usec = time * 1000 * 1000 / _freq;
  const jlong msec = usec / 1000;
  s->print( "%6d", jint(msec) );
  if( _use_usec ) {
    usec %= 1000;
    s->print(".");
    if( usec < 100 ) {
      s->print("0");
    }
    if( usec < 10 ) {
      s->print("0");
    }
    s->print("%d", usec);
  }

  s->print_cr(" %8d %s", jint(time), name() );
  return time;
}


EventLogger::Block*  EventLogger::Block::_head;
EventLogger::Block** EventLogger::Block::_tail;
int                  EventLogger::Block::_used;

inline void EventLogger::Block::initialize ( void ) {
  _head = NULL;
  _tail = &_head;
  _used = size;
}

inline void EventLogger::Block::terminate ( void ) {
  for( Block* block = _head; block; ) {
    Block* next = block->_next;
    OsMemory_free( block );
    block = next;
  }
}

inline void EventLogger::Block::allocate ( void ) {
  Block* block = (Block*)OsMemory_allocate( sizeof( Block ) );
  block->_next = NULL;
  *_tail = block;
  _used = 0;
}

inline void EventLogger::Block::overflow( void ) {
  allocate();
}

inline int EventLogger::Block::used ( void ) const {
  return _next ? size : _used;
}

inline void EventLogger::Block::log ( const EventType type ) {
  if( _used == size ) {
    overflow();
  }
  const jlong time = EventLogger::Entry::now();
  (*_tail)->_entries[_used++].set( type, time );
}

inline jlong
EventLogger::Block::dump( Stream* s, jlong time ) const {
  const int count = used();
  for( int i = 0; i < count; i++) {
    time = _entries[i].dump( s, time );
  }
  return time;
}


void EventLogger::initialize( void ) {
  EventLogger::Entry::initialize();
  EventLogger::Block::initialize();
}

void EventLogger::log(const EventType type) {
  if( UseEventLogger ) {
    EventLogger::Block::log( type );
  }
}

void EventLogger::dump( void ) {
  if (!UseEventLogger) {
    return;
  }
  if( LogEventsToFile ) {
    static const JvmPathChar filename[] = {
      'e','v','e','n','t','.','l','o','g',0
    };
    FileStream s(filename, 200);
    dump(&s);
  } else {
    dump(tty);
  }
}
  
void EventLogger::dump( Stream* s ) {
  s->print_cr("*** Event log:");
  s->print( Entry::use_usec() ? "      msec" : "  msec" );
  s->print_cr("   hrtick event");
  s->print_cr("=======================================");

  jlong time = 0;
  for( const Block* block = Block::_head; block; block = block->_next ) {
    time = block->dump( s, time );
  }
  s->print_cr("=======================================");
}

void EventLogger::dispose( void ) {
  EventLogger::Block::terminate();
  EventLogger::Entry::terminate();
}

#endif // USE_EVENT_LOGGER

void JVM_LogEvent(int type) {
  GUARANTEE( unsigned(type) <= EventLogger::SCREEN_UPDATE_END,
             "Wrong event type" );
  EventLogger::log( EventLogger::EventType( type ) );
}

