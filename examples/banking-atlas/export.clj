(ns export
  "The banking example as atlas's registry: what meta-source merges from
   Overarch's model and atlas's facts, with atlas's own ontologies for the
   types the facts use, written as canonical EDN into a git store. Each
   export that changes something is a commit, so `bb registry` answers
   about any of them, and diffs them."
  (:require [atlas.meta-source :as ams]
            [atlas.registry :as registry]
            [atlas.store.canonical :as canon]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [meta-source.cli :as cli]
            [meta-source.index :as index]))

(def root (str (fs/canonicalize "../..")))
(def store (str (fs/path root ".atlas-store")))

(defn- builtin-ontologies
  "atlas's ontologies for the built-in types the facts use, registered
   as their namespaces load."
  []
  (reset! registry/registry {})
  (require 'atlas.ontology.interface-endpoint 'atlas.ontology.structure-component :reload)
  @registry/registry)

(defn -main [& _]
  (let [idx (index/build (cli/config {:root root}))
        merged (ams/registry-of (update-vals (:entities idx) :entry))
        reg (canon/sanitize (merge (builtin-ontologies) merged))
        files (canon/registry->files reg)
        git (fn [& args] (apply shell {:dir store :out :string :err :string} "git" args))]
    (when-not (fs/exists? (fs/path store ".git"))
      (fs/create-dirs store)
      (git "init" "-q"))
    (fs/delete-tree (fs/path store "entities"))
    (doseq [[path content] files]
      (fs/create-dirs (fs/parent (fs/path store path)))
      (spit (str (fs/path store path)) content))
    (git "add" "-A")
    (if (zero? (:exit (shell {:dir store :continue true} "git" "diff" "--cached" "--quiet")))
      (println "atlas store unchanged:" (count merged) "entities from the example")
      (do (git "-c" "user.name=export" "-c" "user.email=export@localhost" "commit" "-qm"
               (str "export: " (count merged) " entities"))
          (println "atlas store" store ":" (count merged) "entities from the example,"
                   (count reg) "with atlas's ontologies")))))
