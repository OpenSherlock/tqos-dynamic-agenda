/**
 * 
 */
package org.topicquests.tuplespace.util;

//package edu.bloomu.chap9.sect2;

import java.util.ArrayList;

/**
 * Sorted list of arbitrary comparable objects.
 *
 * @author Drue Coles
 */
public class SortedList {

   private final ArrayList<Comparable> list = new ArrayList<>();

   /**
    * Returns the size of this list.
    */
   public int size() {
      return list.size();
   }

   /**
    * Returns the item at a given position of the list.
    */
   public Comparable get(int i) {
      return list.get(i);
   }

   /**
    * Adds an item to the list at correct position in sorted order.
    */
   public void add(Comparable itemToAdd) {
      int insertionPoint = 0;
      while (insertionPoint < size()) {
         Comparable listItem = get(insertionPoint);
         if (itemToAdd.compareTo(listItem) <= 0) { // late binding 
            break;
         }
         insertionPoint++;
      }
      list.add(insertionPoint, itemToAdd);
   }
   
   public void put(Comparable itemToAdd) {
	   add(itemToAdd);
   }

}