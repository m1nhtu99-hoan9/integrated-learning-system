(ns user.data-seeding
  (:require
    [integrated-learning-system.handlers.accounts :as h-accounts]
    [integrated-learning-system.handlers.timeslots :as h-timeslots]))


(defonce data
         {:admins    [{:username "admin", :password "hashed-password"}]
          :teachers  [{:username      "teacher", :password "todo-hashed-password"
                       :first-name    "John the Teacher", :last-name "Nguyễn",
                       :date-of-birth "01/01/1980", :personal-email "john_doe.teacher@domain.com", :phone-number "+849041234568"},
                      {:username   "teacher1", :password "hashed-password"
                       :first-name "Teacher-1", :last-name "Nguyễn", :date-of-birth "02/01/1981"},
                      {:username   "teacher2", :password "hashed-password"
                       :first-name "Teacher-1", :last-name "Phạm", :personal-email "teacher2_fpt@gmail.com"}]
          :students  [{:username      "student", :password "hashed-password",
                       :first-name    "Tommy the Student", :last-name "Hoàng",
                       :date-of-birth "21/12/1996", :personal-email "tommy_learner_for_life@domain.com", :phone-number "+84904123457"},
                      {:username   "student1", :password "hashed-password"
                       :first-name "Ken", :last-name "Đặng", :date-of-birth "02/11/1997"},
                      {:username   "student2", :password "hashed-password"
                       :first-name "Hân", :last-name "Hoàng", :personal-email "hanhoang97@gmail.com"},
                      {:username   "dataStudent1", :password "hashed-password"
                       :first-name "Data", :last-name "Engineer"},
                      {:username   "softwareStudent1", :password "hashed-password"
                       :first-name "Software", :last-name "Engineer"}]
          :timeslots [{:start-at "07:30", :duration-mins 90},
                      {:start-at "09:10", :duration-mins 90},
                      {:start-at "10:50", :duration-mins 90},
                      {:start-at "14:00", :duration-mins 90},
                      {:start-at "15:40", :duration-mins 90}]})


(defn add-teacher! [db-conn req-body]
  (h-accounts/register-account {:services    {:db-conn db-conn}
                                :body-params (assoc req-body :role "teacher")}))

(defn add-student! [db-conn req-body]
  (h-accounts/register-account {:services    {:db-conn db-conn}
                                :body-params (assoc req-body :role "student")}))

(defn add-admin! [db-conn req-body]
  (h-accounts/register-account {:services    {:db-conn db-conn}
                                :body-params (assoc req-body :role "admin")}))

(defn add-timeslot! [db-conn req-body]
  (h-timeslots/insert-timeslot {:services    {:db-conn db-conn}
                                :body-params req-body}))

(defn seed-users! [db-conn {:as data, :keys [admins teachers students]}]
  (concat
    (mapv #(add-teacher! db-conn %) teachers)
    (mapv #(add-student! db-conn %) students)
    (mapv #(add-admin! db-conn %) admins)))

(defn seed-timeslots! [db-conn timeslots]
  (mapv #(add-timeslot! db-conn %) timeslots))
