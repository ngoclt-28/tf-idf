(ns tf-idf.core
  (:require [clojure.java.io :as io]
            [clojure.string :as st])
  (:gen-class))

(def non-word-regex
  "regular expression for finding all non-word characters and leaving spaces for delimiting"
  #"(?![a-zA-Z0-9À-ÿ\s])(\W)")

(defn get-basename-string [s]
  "get basename of (string) file"
  (re-find (re-pattern "^[a-zA-Z0-9À-ÿ ()]+") s))
  ;; (re-find (re-pattern ".*[^.txt]") s))

(defn clean-file [f]
  "remove non-word characters and replace with '' and change everything to lowercase in a file"
  (-> (slurp f)
      (st/replace non-word-regex "")
      (st/replace #"\s" " ")
      st/lower-case))

(defn get-terms-list [s]
  "delimit string by space (' ') and remove '' characters"
  (remove #{""} (st/split s #" ")))

(defn get-in-terms [m]
  "return the terms associated with a file"
  (get-in m [:terms]))

(defn per-term-doc-count [m]
  "make list of all keys from nested maps"
  (frequencies (flatten (conj '() (map keys (map get-in-terms m))))))

(defn sort-tf-idf [m]
  "sort tf-idf values for each file from high to low"
  (into {} #{(sort-by val > m)}))

(defn calculate-tf [m]
  "divide occurrences of a term by the total number of terms in a single document"
  (reduce-kv (fn [n k v] (assoc n k (float (/ v (count m))))) (empty m) m))

(defn calculate-idf [m c]
  "divide total number of documents by number of documents with term. then, take the log_e"
  (reduce-kv (fn [n k v] (assoc n k (Math/log (float (/ c v))))) (empty m) m))

(defn calculate-tf-idf [tf idf]
  "calculate tf-idf (tf * idf) for a term"
  (let [file-name (get tf :file)
        tf-idf (reduce-kv (fn [n k v] (assoc n k (* v (get idf k)))) (empty tf) (get-in-terms tf))
        sorted-tf-idf (sort-tf-idf tf-idf)]
    {:file file-name
     :tf-idf sorted-tf-idf}))

(defn get-tf [f]
  "remove punctuation from file. get all terms from file. get occurrences of each term. calculate term frequency."
  (let [file-name (.getName f)
        file (clean-file f)
        term-list (get-terms-list file)
        term-counts (frequencies term-list)]
    {:file file-name
     :terms (calculate-tf term-counts)}))

(defn get-idf [m]
  "count number of documents term occurs in then calculate inverse document frequency for terms"
  (-> (per-term-doc-count m)
    (calculate-idf (count m))))

(defn get-tf-idf [m n]
  "loop through all term-frequency maps and calculate tf-idf for each one"
  (loop [tf-idf-map (empty '())
         l (first m)
         r (rest m)]
    (if l
      (recur (conj tf-idf-map (calculate-tf-idf l n))
             (first r)
             (rest r))
      tf-idf-map)))

(defn output-to-file [m]
  "write each term and respective tf-idf value in the respective output file (one per line comma-delimited)"
  (let [base-file-name (get-basename-string (get m :file))
        output-file (str (System/getProperty "user.dir") "/output/" base-file-name "-output.csv")]
    (.mkdir (io/file (.getParent (io/file output-file))))
    (with-open [wrtr (io/writer output-file)]
      (doseq [[k v] (get m :tf-idf)]
        (.write wrtr (str k "," v "\n"))))))

(defn -main [& args]
  "calculate tf-idf for terms in a user-provided directory and write to files in an output directory"
  (println "Provide a directory:")
  (let [files (.listFiles (io/file (read-line)))
        term-tf (doall (map get-tf files))
        all-terms (per-term-doc-count term-tf)
        term-idf (get-idf term-tf)
        term-tf-idf (get-tf-idf term-tf term-idf)]
    ;; term-tf-idf))
    (pmap output-to-file term-tf-idf)))
