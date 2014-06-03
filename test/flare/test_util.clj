(ns flare.test-util
  (:require [clojure.test :refer :all]
            [flare.util :as f-util]))

(deftest get-queues-test
  (testing "get some queues"
    (is (= {:flare {:queues [:genius :moodle :showevidence]}}
           (f-util/get-queues {})))))

