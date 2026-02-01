(ns dev.debug-test)

;; A function that should return the second largest number in a collection
;; But it has a bug!
(defn second-largest
  "Returns the second largest number in the collection.
   Returns nil if the collection has fewer than 2 unique elements."
  [nums]
  (let [sorted-desc (sort > (distinct nums))]
    (when (>= (count sorted-desc) 2)
      (nth sorted-desc 1))))

;; A function that should calculate the average of numbers
(defn average
  "Calculates the average of a collection of numbers."
  [nums]
  (if (empty? nums)
    0
    (/ (reduce + nums) (count nums))))

;; A function that should find numbers above average
(defn above-average
  "Returns all numbers in the collection that are above the average."
  [nums]
  (let [avg (average nums)]
    (filter #(% > avg) nums)))

;; A process function that chains these operations
(defn process-numbers
  "Process a collection of numbers:
   1. Find the second largest
   2. Calculate the average
   3. Find numbers above average"
  [nums]
  {:second-largest (second-largest nums)
   :average        (average nums)
   :above-average  (above-average nums)})

;; Test cases
(comment
  ;; These should work correctly:
  (process-numbers [1 2 3 4 5])
  ;; Expected: {:second-largest 4, :average 3, :above-average (4 5)}

  ;; This might expose the bug:
  (process-numbers [5 5 5 3 2])
  ;; Expected: {:second-largest 3, :average 4, :above-average (5 5 5)}

  ;; Edge cases:
  (process-numbers [])
  ;; Expected: {:second-largest nil, :average 0, :above-average ()}

  (process-numbers [1])
  ;; Expected: {:second-largest nil, :average 1, :above-average ()}

  (process-numbers [1 1 1])
  ;; Expected: {:second-largest nil, :average 1, :above-average ()}
  )
