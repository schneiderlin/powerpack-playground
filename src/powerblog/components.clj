(ns powerblog.components)

(def navbar
  [:nav {:class "bg-surface-elevated border-b border-border-default sticky top-0 z-50"}
   [:div {:class "layout-container"}
    [:div {:class "flex items-center justify-between h-14"}
     ;; Logo section
     [:a {:href "/" :class "flex items-center gap-3 group"}
      [:div {:class "w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center"}
       [:svg {:class "w-5 h-5 text-white" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
        [:path {:d "M4 4h16v12H4z"}]
        [:path {:d "M8 20h8"}]
        [:path {:d "M12 16v4"}]]]
      [:span {:class "text-lg font-semibold text-gray-900 group-hover:text-primary-600 transition-colors duration-200"}
       "LinZiHao"]]

     ;; Desktop Navigation links
     [:div {:class "hidden md:flex items-center gap-1"}
      [:a {:href "/"
           :class "px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 hover:text-primary-600 transition-colors duration-200"}
       "Home"]
      [:a {:href "/about"
           :class "px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 hover:text-primary-600 transition-colors duration-200"}
       "About"]]

     ;; Mobile menu button
     [:button {:type "button"
               :class "md:hidden p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-md transition-colors duration-200"
               :aria-controls "navbar-default"
               :aria-expanded "false"
               :id "mobile-menu-button"}
      [:span {:class "sr-only"} "打开菜单"]
      [:svg {:class "w-5 h-5" :aria-hidden "true" :xmlns "http://www.w3.org/2000/svg"
             :fill "none" :viewBox "0 0 17 14"}
       [:path {:stroke "currentColor" :stroke-linecap "round" :stroke-linejoin "round"
               :stroke-width "2" :d "M1 1h15M1 7h15M1 13h15"}]]]]

    ;; Mobile Navigation menu
    [:div {:class "hidden md:hidden border-t border-border-default py-2" :id "navbar-default"}
     [:div {:class "flex flex-col gap-1"}
      [:a {:href "/" :class "px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 hover:text-primary-600 transition-colors duration-200"}
       "Home"]
      [:a {:href "/about" :class "px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 hover:text-primary-600 transition-colors duration-200"}
       "About"]]]]])

(def navbar-script
  "document.getElementById('mobile-menu-button').addEventListener('click', function() {
    var menu = document.getElementById('navbar-default');
    menu.classList.toggle('hidden');
  });")

(def footer
  [:footer {:class "w-full bg-surface-elevated border-t border-border-default mt-auto"}
   [:div {:class "layout-container py-8"}
    [:div {:class "flex flex-col items-center justify-center gap-6"}
     ;; Social links
     [:nav {:class "flex flex-wrap justify-center gap-6"}
      [:a {:href "https://www.linkedin.com/in/zihao-lin-8b8067326/"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"}
       "LinkedIn"]
      [:a {:href "https://x.com/fTJSaF2VnI11762"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"}
       "X (Twitter)"]
      [:a {:href "https://space.bilibili.com/375039815"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"}
       "Bilibili"]
      [:a {:href "https://www.zhihu.com/people/lin-zi-hao-8-89"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"}
       "Zhihu"]
      [:a {:href "https://github.com/schneiderlin"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-sm font-medium text-gray-500 hover:text-gray-900 transition-colors duration-200"}
       "GitHub"]]
     ;; Copyright
     [:p {:class "text-sm text-gray-400"}
      (str "© " (.getYear (java.time.LocalDate/now)) " LinZiHao. All rights reserved.")]]]])
