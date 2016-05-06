(ns iwaswhere-web.store
  (:require [iwaswhere-web.imports :as i]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.graph :as g]
            [ubergraph.core :as uber]
            [clojure.pprint :as pp]))

(defn state-get-fn
  "Handler function for retrieving current state."
  [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:last-filter] msg-payload)]
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state msg-payload)]}))

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state.
  Entries are stored as attributes of graph nodes, where the node itself is
  timestamp of an entry. A sort order by descending timestamp is maintained
  in a sorted set of the nodes."
  [path]
  (fn
    [_put-fn]
    (let [state (atom {:sorted-entries (sorted-set-by >)
                       :graph          (uber/graph)
                       :last-filter    {}})
          files (file-seq (clojure.java.io/file path))]
      (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn")]
        (let [lines (line-seq (clojure.java.io/reader f))]
          (doseq [line lines]
            (let [parsed (clojure.edn/read-string line)
                  ts (:timestamp parsed)]
              (if (:deleted parsed)
                (swap! state g/remove-node ts)
                (swap! state g/add-node ts parsed))))))
      {:state state})))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (state-fn "./data/daily-logs")
   :handler-map {:geo-entry/persist f/geo-entry-persist-fn
                 :geo-entry/import  f/entry-import-fn
                 :text-entry/update f/geo-entry-persist-fn
                 :cmd/trash         f/trash-entry-fn
                 :state/get         state-get-fn}})
