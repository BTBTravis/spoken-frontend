(ns reagenttest.core
  (:require
    [reagent.core :as r]
    [clojure.string :as str]
    [taoensso.timbre :as timbre :refer [spy]]
    ;[socket.io-client :as io]
  )
)

;(def socket (io "http://localhost"))

(def log (.-log js/console))
; TODO: Move randomColor to backend
(defn randomColor []
  (str "hsl(" (rand-int 360) ", 96%, 55%)")
)
;var elem_top = $("#my_item").offset()['top'];
;var viewport_height = $(window).height();

; Scroll to the current word is middle of the viewport
(defn center []
  (let [yDis (.-offsetLeft (.-parentElement (.querySelector js/document "p.current.text")))
        width (.-innerWidth js/window)
        el (.querySelector js/document "div.wordstream")]
    (-> (spy (- yDis (/ width 2) ))
        ;(spy {:yDis yDis :width width :el el})
        ;(set! (.-name my-type) "Andy")
        (#(spy (set! (.-scrollLeft el) %)))
        (#(spy [yDis width el])))
  )
)

; Data
(def faketxt "The quick brown fox jumped over the lazy dog. Little did the fox know the dog was laying a trap for him. As soon as the foxed landed a snap of jaws and this rear leg was mangled")
(def updatesAtom (r/atom {})); Atom to hold state of the story init loaded from ajax call then updated over websocket
; Load inital updates from encoded json on data attr somewhere... 
(->
  (.get js/axios "http://localhost:3000/getallupdates"); Make ajax get call to backend to get the big list of updates 
  (#(.then % (fn [x] (-> 
    (js->clj x :keywordize-keys true); Convert the server responce from json to clj map with usable keys
    ((fn [d] (:data d))); get the data key
    ((fn [d] (:res d))); get the res key
    ((fn [d] (spy (swap! updatesAtom (fn [old] (identity d)))))); Update the updateAtom with its some state
    ;((fn [d] (centerWord)))
    ; TODO: scroll to correct pos
    ))))
)

(def socket (js/io "http://localhost:3000"))
(.on socket "connect" #(println "Connected to socket"))
;var socket = io('http://localhost');
  ;socket.on('connect', function(){});
  ;socket.on('event', function(data){});
  ;socket.on('disconnect', function(){});
; data processing
(defn txtfragmnets; get text fragments from a string and updates or entries that were maid aginst that string
  [txt updates]  
  (->
    (->> (str/split txt #"\s"); split txt up by space
         (map #(identity {:word %})); create a map out of these words ex. {:word "hello"}
         (map-indexed (fn [i obj]  (conj obj {:i i}))); insert the index of the word into the map ex. {:word "hello" :i 0}
         ; bind the correct user to this object or nil ex. {:word "hello" :i 0 :user "travis002"}
         (map #(conj 
          % 
          (reduce (fn [user val] 
            (if (and (>= (:end val) (:i %)) (<= (:start val) (:i %)))
             val 
             user
            )
          )  nil updates)
         ))
    )
    ((fn [words]; split fragmnets into seprate seq based on user  
      (conj
        (reverse (map (fn [x] (reverse (drop (:start x) (take (+ 1 (:end x)) words)))) updates))
        (take 1 (drop (+ (:end (last updates)) 1) words))
        (reverse (lazy-seq (drop (+ (:end (last updates)) 2) words)))
      )
    ))
    (reverse); idk why the reverse calls are needed
    ((fn [x] (map #(assoc (first %) :word (reduce (fn [carry val] (str (:word val) " "  carry)) "" %)) x ))); join words into one long string joinning with space
    ((fn [x] (map #(dissoc % :i) x))); remove the :i key it is no longer needed
    (#(into [] %)); lazy seq -> map
    (#(assoc-in % [(- (count %) 1) :type] "future")); attach future key to last element in vector
    (#(assoc-in % [(- (count %) 2) :type] "current")); attach current key to second to last element in vector
    ((fn [frags] (map-indexed (fn [i frag] (assoc frag :key i)) frags))) ;set unique keys
  )
)

(println "CLEAR")
;(.log js/console "Hello, world!")
;(cljs.pprint/pprint (txtfragmnets faketxt fakeupdates))
;(cljs.pprint/pprint (count (txtfragmnets faketxt fakeupdates)))
;(cljs.pprint/pprint (type (txtfragmnets faketxt fakeupdates)))
;(cljs.pprint/pprint (map #(type %) (txtfragmnets faketxt fakeupdates)))

(defn home-page []
  [:div 
    [:div.btns 
     [:button { :on-click 
        #(swap! updatesAtom (fn [old]
         (conj old {:userstr "kelsey 11/01/17" :start 18 :end 22 :color (randomColor) } )
        ))
      } "speak"]
     [:button {:on-click #(cljs.pprint/pprint @updatesAtom)} "view"]
     [:button {:on-click #(center)} "center"]
    ]
    [:div.wordstream (map #(identity 
      [:div {:key (:key %)} 
        [:p.text {:class (:type %)} (:word %)]
        (when (not= nil (:color %)) [:div.bar {:style {:border-top-color (:color %)}}])
        (when (not= nil (:userstr %)) [:p.user {:style {:color (:color %)}} (:userstr %)])

      ]) (txtfragmnets faketxt @updatesAtom ))]
    [:div.progressbar [:p "progressbar"]]
    [:div.rankings [:p "rankings"]]
  ]
)


;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
