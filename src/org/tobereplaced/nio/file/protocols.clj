(ns org.tobereplaced.nio.file.protocols
  (:import (java.io File InputStream OutputStream)
           (java.net URI)
           (java.nio.charset Charset StandardCharsets)
           (java.nio.file CopyOption FileSystem Files OpenOption Path
                          Paths)))

(def ^:private empty-string-array (into-array String []))

(defprotocol UnaryPath
  (unary-path [this]))

(extend-protocol UnaryPath
  Path
  (unary-path [this] this)
  File
  (unary-path [this] (.toPath this))
  URI
  (unary-path [this] (Paths/get this))
  String
  (unary-path [this] (Paths/get this empty-string-array)))

(defprotocol NaryPath
  (nary-path [this more]))

(extend-protocol NaryPath
  FileSystem
  (nary-path [this [s & more]] (.getPath this s (into-array String more)))
  String
  (nary-path [this more] (Paths/get this (into-array String more))))

;; Private implementation protocol to allow for dispatch when it is
;; known that we are copying from an input stream.
(defprotocol ^:private CopyFromInputStream
  (copy-from-input-stream [this source options]))

(extend-protocol CopyFromInputStream
  Path
  (copy-from-input-stream [this ^InputStream source options]
    (Files/copy source this
                ^"[Ljava.nio.file.CopyOption;"
                (into-array CopyOption options)))
  Object
  (copy-from-input-stream [this source options]
    (copy-from-input-stream (unary-path this) source options)))

;; Private implementation protocol to allow for dispatch when it is
;; known that we are copying from a path.
(defprotocol ^:private CopyFromPath
  (copy-from-path [this source options]))

(extend-protocol CopyFromPath
  OutputStream
  (copy-from-path [this source _]
    (Files/copy source this))
  Path
  (copy-from-path [this ^Path source options]
    (Files/copy source this
                ^"[Ljava.nio.file.CopyOption;"
                (into-array CopyOption options)))
  Object
  (copy-from-path [this source options]
    (copy-from-path (unary-path this) source options)))

(defprotocol Copy
  (copy [this target options]))

(extend-protocol Copy
  InputStream
  (copy [this target options]
    (copy-from-input-stream target this options))
  Path
  (copy [this target options]
    (copy-from-path target this options))
  Object
  (copy [this target options]
    (copy (unary-path this) target options)))

;; Private implementation protocol to allow for dispatch based on
;; whether a charset is provided or not.
(defprotocol ^:private WriteLines
  (write-lines [this path lines options]))

(extend-protocol WriteLines
  Charset
  (write-lines [this path lines options]
    (Files/write path lines this (into-array OpenOption options)))
  OpenOption
  (write-lines [this path lines options]
    (write-lines StandardCharsets/UTF_8 path lines (cons this options)))
  nil
  (write-lines [_ path lines options]
    (write-lines StandardCharsets/UTF_8 path lines options)))

(defprotocol Write
  (write [this path options]))

(extend-protocol Write
  (Class/forName "[B")
  (write [this ^Path path options]
    (Files/write path
                 ^"[B" this
                 ^"[Ljava.nio.file.OpenOption;"
                 (into-array OpenOption options)))
  Iterable
  (write [this path options]
    (write-lines (first options) path this (rest options))))
