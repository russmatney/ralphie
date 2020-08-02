(ns ralphie.item)


(defn awesome-tag-parent? [item]
  (-> item
      :props
      :child-tag
      (= "awesome-tag")))


(defn ->level-1-list [root-item pred]
  (some-> root-item
          :items
          (filter pred)
          first
          :items))
