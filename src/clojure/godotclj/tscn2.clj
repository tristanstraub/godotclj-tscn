(ns godotclj.tscn2
  (:require [instaparse.core :as instaparse]
            [clojure.string :as str]
            [clojure.pprint :refer [cl-format]]
            [flatland.ordered.map :refer [ordered-map]]))

(defn parser
  [s & args]
  (let [p (apply instaparse/parser
                 "
    file       = <ws*> global? <ws*> descriptor (<ws*> descriptor <ws*>)*;
    global     = attributes?
    descriptor = header (<ws*> kv)*;
    header     = <'['> <ws*> identifier (<ws+> attributes)? <ws*> <']'>;
    attributes = kv (<ws+> kv)*;
    kv         = identifier <ws*> <'='> <ws*> value;
    identifier = #'[0-9a-zA-Z_/.]+';
    value      = integer | number | object | string | dictionary | boolean | array;
    boolean    = 'true' | 'false';
    object     = identifier <'('> <ws*> arguments? <ws*> <')'>;
    integer    = #'[+-]'? #'[0-9]+';
    number     = #'[+-]'? #'[0-9]+' '.' #'[0-9]+';
    string     = <'\"'> #'[^\"]*' <'\"'>;
    arguments  = value (<ws*> <','> <ws*> value)* (<ws*> <','>)?;
    dictionary = <'{'> keyvalues <ws*> <'}'>;
    keyvalues  = (<ws*> dictionarykv <ws*> (<','> <ws*> dictionarykv)*)?;
    array      = <'['> <ws*> arguments? <ws*> <']'>;
    dictionarykv = string <ws*> <':'> <ws*> value;
    ws         = #'[ \t\n]+' | ';' #'[^\n]*[\n]?';
"
                 args)]
    (p s)))

(defrecord TObject [])
(defrecord TDictionary [])

(def transforms
  {:file         (fn [& parts] (vec parts))
   :kv           (fn [k v]
                   [(name k) v])
   :descriptor   (fn [resource & values]
                   (if (seq values)
                     (assoc resource :children (into (ordered-map) values))
                     resource))
   :global       (fn
                   ([attributes]
                    {:children attributes}))
   :header       (fn
                   ([resource]
                    {:resource   {:name resource}})
                   ([resource attributes]
                    {:resource   {:name resource :attributes attributes}}))
   :identifier   (fn [vs] vs)
   :value        (fn [vs] vs)
   :attributes   (fn [& kv] (into (ordered-map) kv))
   :dictionary   (fn
                   ([] nil)
                   ([kv] (map->TDictionary (into (ordered-map) kv))))
   :keyvalues    (fn [& kv] kv)
   :array        (fn
                   ([] [])
                   ([values] (vec values)))
   :dictionarykv (fn [k v]
                   [(name k) v])

   :number       (fn [& value]
                   (Double/parseDouble (apply str value)))
   :integer      (fn [& value]
                   (Integer/parseInt (apply str value)))
   :string       (fn [v] v)
   :boolean      (fn [v] (= v "true"))
   :object       (fn [name args]
                   (map->TObject {:name name :arguments args}))
   :arguments    (fn [& args]
                   (vec args))})

(defn parse
  [s & args]
  (instaparse/transform transforms (apply parser s args)))

(declare emit-value)

(defn emit-arguments
  [options arguments]
  (str/join (get-in options [:arguments :separator])
            (map (partial emit-value options) arguments)))

(defn emit-object
  [options {:keys [name arguments]}]
  (str name "( " (emit-arguments options arguments) " )"))

(declare emit-attributes)

(defn emit-dictionary
  [options vs]
  (str "{" (emit-attributes (-> options
                                (assoc-in [:attribute :string-key?] true)
                                (assoc-in [:attribute :operator] ":")
                                (assoc-in [:attributes :separator] ",\n"))
                            vs) "}"))

(defn emit-value
  [options v]
  (cond (string? v)               (cl-format nil "\"~a\"" v)
        (instance? TObject v)     (emit-object options v)
        (instance? TDictionary v) (emit-dictionary options v)
        (vector? v)               (str "[" (emit-arguments options v) "]")

        :else                     v))

(defn emit-attribute
  [options [k v]]
  (cl-format nil "~a~a~a~a~a"
             (if (get-in options [:attribute :string-key?])
               (cl-format nil "\"~a\"" (name k))
               (name k))
             (get-in options [:attribute :spacing])
             (get-in options [:attribute :operator])
             (get-in options [:attribute :spacing])
             (emit-value options v)))

(defn emit-attributes
  [options attributes]
  (str/join (get-in options [:attributes :separator])
            (map (partial emit-attribute options) attributes)))

(defn emit-resource-header
  [options {:keys [name attributes] :as header}]
  (let [options (assoc-in options [:attribute :spacing] "")]
    (if (seq attributes)
      (cl-format nil "[~a ~a]" name (emit-attributes options attributes))
      (cl-format nil "[~a]" name))))

(defn emit-block
  [options {:keys [resource children]}]
  (cond-> (str/join (get-in options [:blocks :spacing])
                    `[~@(when resource
                          [(str (emit-resource-header options resource)
                                (get-in options [:block :header :spacing]))])
                      ~(emit-attributes (assoc-in options [:attributes :separator] "\n")
                                        children)])
    (seq children)
    (str "\n\n")))

(def default-options
  {:arguments  {:separator ", "}
   :attribute  {:spacing     " "
                :string-key? false
                :operator    "="}
   :attributes {:separator " "
                :suffix    ""}
   :block      {:header {:spacing "\n"}}
   :blocks     {:spacing   "\n"
                :separator ""}})

(defn emit-blocks
  [options blocks]
  (str/join (get-in options [:blocks :separator])
            (map (partial emit-block options) blocks)))

(defn emit
  ([options blocks]
   (emit-blocks options blocks))
  ([blocks]
   (emit default-options blocks)))

(comment
  (instaparse/transform transforms (parser "[gd_scene load_steps=3 format=2]"))

  (parse "[gd_scene load_steps=3 format=2]" :start :header)

  (parse "[gd_scene load_steps=3 format=2]" :start :header)

  (parse (slurp "../thecreeps-godot-clj/src/godot/HUD.tscn"))

  (print (emit-blocks default-options (parse (slurp "../thecreeps-godot-clj/src/godot/HUD.tscn"))))
  (print (emit-blocks default-options (parse (slurp "../thecreeps-godot-clj/src/godot/libgodotclj.gdnlib"))))
  (print (emit-blocks default-options (parse (slurp "../thecreeps-godot-clj/project.godot"))))
  (print (emit-blocks default-options (parse (slurp "../thecreeps-godot-clj/src/godot/main.tscn"))))
  (print (emit-blocks default-options (parse (slurp "../thecreeps-godot-clj/src/godot/main.gdns"))))

  (parse (slurp "../thecreeps-godot-clj/project.godot"))

  (parse (slurp "../thecreeps-godot-clj/src/godot/libgodotclj.gdnlib"))


  (parse "{\n}" :start :dictionary)

  (parse "config_version=4" :start :attributes)

  )
