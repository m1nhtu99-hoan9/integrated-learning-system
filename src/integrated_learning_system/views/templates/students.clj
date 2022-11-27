(ns integrated-learning-system.views.templates.students
  (:require
    [hiccup.page :refer [html5]]
    [integrated-learning-system.views.commons :as commons]
    [java-time.api :as jt])
  (:use [hiccup.core]))


(defn all-students-page [{:keys [students]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title "All students"})
    (commons/body
      {:navbar-props nil}
      [:main
       [:section.hero.is-link {:id "hero-banner"}
        [:div.hero-body
         [:p.title "Students"]]]
       [:section.container.is-fluid {:id "students-content-container"}
        [:div.content
         [:p]
         [:table.is-fullwidth.is-striped {:class "table"}
          [:tr
           [:th [:span "Student Name (Username)"]]
           [:th [:span "Date of Birth"]]
           [:th [:span "Personal Email"]]
           [:th [:span "Phone Number"]]]
          (for [student students,
                :let [{:keys [username full-name personal-email date-of-birth phone-number uris]} student,
                      {timetable-uri :timetable} uris,
                      dob-string (some->> date-of-birth (jt/format "dd/MM/uuuu"))]]
            [:tr
             [:td [:span
                   [:a {:href timetable-uri} full-name]
                   (str " (" username ")")]]
             [:td dob-string]
             [:td personal-email]
             [:td phone-number]])]]]])))
