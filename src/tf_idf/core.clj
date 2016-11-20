(ns tf-idf.core
  (:require [clojure.java.io :as io]
            [clojure.string :as st])
  (:gen-class))

;; currently removes accented characters like: â, ê, etc.
(def non-word-regex #"[^(? )\w]")

(defn clean-file [f]
  (st/lower-case (st/replace (slurp f) non-word-regex "")))

(defn get-terms-list [s]
  (st/split s #" "))

(defn per-term-doc-count [m]
  "make list of all keys from nested maps"
  (frequencies (flatten (conj '() (map keys m)))))

(defn calculate-tf [m]
  "divide occurrences of a term by the total number of terms in a single document"
  (reduce-kv (fn [n k v] (assoc n k (float (/ v (count m))))) (empty m) m))

(defn calculate-idf [m c]
  "divide total number of documents by number of documents with term. then, take the log_e"
  (reduce-kv (fn [n k v] (assoc n k (Math/log (float (/ c v))))) (empty m) m))

(defn calculate-tf-idf [tf idf]
  "calculate tf-idf (tf * idf) for a term"
  (reduce-kv (fn [n k v] (assoc n k (* v (get idf k)))) (empty tf) tf))

(defn get-tf [f]
  "remove punctuation from file. get all terms from file. get occurrences of each term. calculate term frequency."
  (let [file (clean-file f)
        term-list (get-terms-list file)
        term-counts (frequencies term-list)]
    (calculate-tf term-counts)))

(defn get-idf [m]
  (let [term-doc-count (per-term-doc-count m)]
    (calculate-idf term-doc-count (count m))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Provide a directory:")
  (let [files (.listFiles (io/file (read-line)))
        term-tf (doall (map get-tf files))
        all-terms (per-term-doc-count term-tf)
        term-idf (get-idf term-tf)]
    (calculate-tf-idf (second term-tf) term-idf)))
        ;; term-idf (get-idf term-frequencies (count files))]
    ;; (keys term-frequencies))))
    ;; (doall (map get-tf files))))
