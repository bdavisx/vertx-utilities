/*
 * Copyright (c) 2019 Bill Davis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   - http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tartner.vertx

/** This is a very minimal class, not thread safe at all. */
class DequeForTesting<T>: Iterable<T> {
  class Node<T>(var value: T) {
    var next: Node<T>? = null
    var previous: Node<T>? = null

    override fun toString(): String {
      return "Node(value=$value, next=${next?.value}, previous=${previous?.value})"
    }
  }

  private class DequeIterator<T>(initial: Node<T>?): Iterator<T> {
    private var currentNode: Node<T>? = initial

    override fun hasNext(): Boolean {
      return currentNode != null
    }

    override fun next(): T {
      val current = currentNode
      if (current == null) throw IllegalStateException("No elements!")

      currentNode = current.next
      return current.value
    }
  }

  private var head: Node<T>? = null
  private var tail: Node<T>? = null

  var isEmpty: Boolean = head == null

  fun first(): Node<T>? = head
  fun last(): Node<T>? = tail

  override fun iterator(): Iterator<T> = DequeIterator(first())

  /** Returns the first() item in the deque and removes it. Exception if empty. */
  fun get(): T {
    val head = this.head

    if (head == null) {
      throw IllegalStateException("No items in deque!")
    }

    val newHead = head.next
    if (newHead != null) {
      newHead.previous = null
    } else {
      tail = null
    }

    this.head = newHead

    return head.value
  }

  fun add(value: T) {
    var newNode = Node(value)

    var lastNode = this.last()
    if (lastNode != null) {
      newNode.previous = lastNode
      lastNode.next = newNode
    } else {
      head = newNode
    }
    tail = newNode
  }

  fun removeAll() {
    head = null
    tail = null
  }

  override fun toString(): String {
    var s = "["
    var node = head
    while (node != null) {
      s += "${node.value}"
      node = node.next
      if (node != null) {
        s += ", "
      }
    }
    return s + "]"
  }
}
