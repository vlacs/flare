(ns flare.test-util
  (:require [clojure.test :refer :all]
            [flare.util :as f-util]))

(deftest get-attaches-test
  (testing "get some attaches"
    (is (= {:flare {:attaches [:genius :moodle :showevidence]}}
           (f-util/get-attaches {})))))

