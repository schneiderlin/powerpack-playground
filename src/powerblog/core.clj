(ns powerblog.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [powerpack.hiccup :as hiccup]
   [powerpack.markdown :as md]
   [powerblog.components :as components]))

(comment
  (require '[powerpack.dev :as dev])
  (def app (dev/get-app))
  (def db (d/db (:datomic/conn app)))
  :rcf)

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

(defn get-blog-posts [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :blog-post/author]]
            db)
       (map #(d/entity db %))))

(defn get-posts-by-tag [db tag]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db tag)
       (map #(d/entity db %))))

(comment
  (get-posts-by-tag db :clojure)
  :rcf)

(defn get-all-tags [db]
  (->> (d/q '[:find [?tag ...]
              :where
              [_ :blog-post/tags ?tag]]
            db)
       set))

(defn blog-post-card [post]
  [:a {:href (:page/uri post)
       :class "bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 overflow-hidden block mb-6 break-inside-avoid post"
       :data-tags (clojure.string/join "," (:blog-post/tags post))}
   [:div {:class "p-6"}
    [:h3 {:class "text-center text-xl font-semibold mb-2 text-gray-900"}
     (:page/title post)]
    [:div {:class "text-center mb-4"}
     (for [tag (:blog-post/tags post)]
       [:span {:class "inline-block bg-gray-200 text-gray-700 text-sm font-semibold mr-2 px-2.5 py-0.5 rounded"}
        (name tag)])]
    [:p {:class "text-gray-600 mb-4"}
     (:page/description post "")]
    [:div {:class "flex justify-between items-center text-sm text-gray-500"}
     [:span (:page/date post "")]]]])

(defn tag-filter [all-tags]
  [:div {:class "mb-6"}
   [:h3 {:class "text-xl font-semibold mb-4"} "Filter by tags"]
   [:div {:class "flex flex-wrap gap-2" :id "tag-filter"}
    (for [tag all-tags]
      [:button {:class "px-3 py-1 rounded-full text-sm font-semibold bg-gray-200 text-gray-700"
                :data-tag (name tag)}
       (name tag)])]])

(defn body-to-html-string
  "Render markdown body to a plain HTML string (for regex/TOC and safe embedding via unescape)."
  [body]
  (when (string? body)
    (->> body
         str/split-lines
         md/unindent-but-first
         (str/join "\n")
         (md/md-to-html))))

(defn extract-headings [html]
  (let [pattern #"<h([2-6])[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</h\1>"
        matches (re-seq pattern html)]
    (mapv (fn [[_ depth slug text]]
            {:depth (Integer/parseInt depth)
             :slug slug
             :text (str/replace text #"<[^>]+>" "")})
          matches)))

(defn build-toc [headings]
  (loop [headings headings
         toc []
         parents {}]
    (if-let [h (first headings)]
      (let [depth (:depth h)]
        (cond
          (= depth 2)
          (recur (rest headings)
                 (conj toc (assoc h :subheadings []))
                 (assoc parents depth h))

          :else
          (if-let [parent (get parents (dec depth))]
            (let [new-parent (update parent :subheadings conj h)]
              (recur (rest headings)
                     toc
                     (assoc parents depth new-parent)))
            (recur (rest headings) toc parents))))
      toc)))

(defn render-toc-item [heading]
  (let [base-class (if (= (:depth heading) 2)
                     "block py-2 px-3 text-gray-600 hover:text-gray-800"
                     "block py-1 pl-6 text-sm text-gray-500 hover:text-gray-700")]
    (list
     [:a {:href (str "#" (:slug heading))
          :class base-class
          :data-heading (:slug heading)}
      (:text heading)]

     (when (seq (:subheadings heading))
       [:div
        (for [sub (:subheadings heading)]
          (render-toc-item sub))]))))

(defn layout [{:keys [title description lang]} & content]
  [:html {:lang (or lang "en")}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:meta {:name "description" :content (or description "Blog")}]
    (when title [:title title])
    [:link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"}]
    ;; Design System CSS
    [:link {:rel "stylesheet" :href "/css/styles.css"}]
    ;; Tailwind configuration to match old blog
    [:script "
      tailwind.config = {
        theme: {
          extend: {
            colors: {
              primary: {
                200: '#e0e7ff',
                400: '#818cf8',
              }
            }
          }
        }
      }
    "]
    ;; Progress bar styles
    [:style "
      #progress-bar {
        --progress: 0;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        height: 6px;
        width: var(--progress);
        background-color: #502020;
        z-index: 9999;
      }
    "]]
   [:body {:class "overflow-x-hidden" :style "background-color: #f0f0f0;"}
    components/navbar
    content
    components/footer
    [:script components/navbar-script]
    ;; Progress bar script
    [:script "
      function updateProgressBar() {
        const scrollTop = document.documentElement.scrollTop;
        const scrollHeight = document.documentElement.scrollHeight;
        const scrollPercent = (scrollTop / (scrollHeight - window.innerHeight)) * 100 + '%';

        const element = document.querySelector('#progress-bar');
        if (element) {
          element.style.setProperty('--progress', scrollPercent);
        }
      }

      document.addEventListener('scroll', updateProgressBar);
      window.addEventListener('resize', updateProgressBar);
      updateProgressBar();
    "]]])

(def header
  [:header [:a {:href "/"} "首页"]])

(defn render-frontpage [context page]
  (layout
   {:title "LinZiHao's Digital Garden"
    :description "LinZiHao's Digital Garden"}
   [:main {:class "container mx-auto px-4"}
    ;; Hero section
    [:section {:class "flex flex-col md:flex-row items-center justify-between m-16"}
     [:div {:class "w-full md:w-1/2"}
      [:img {:src "/favicon.svg" :alt "Logo" :class "rounded-lg w-full h-auto"}]]
     [:div {:class "w-full md:w-1/2 md:pl-8 mt-4 md:mt-0"}
      [:h1 {:class "text-4xl md:text-6xl font-mono mb-4"} "Software Engineer"]
      [:h1 {:class "text-4xl md:text-6xl font-serif mb-4"} "AI Enthusiast"]
      [:h1 {:class "text-4xl md:text-6xl font-sans mb-4"} "World Explorer"]
      [:p {:class "text-xl"} "Welcome to my digital garden"]]]

    ;; Blog posts section
    [:section
     [:h2 {:class "text-2xl font-semibold mb-6"} "Articles"]
     (tag-filter (get-all-tags (:app/db context)))
     [:div {:class "columns-1 sm:columns-2 lg:columns-3 gap-6" :id "posts-container"}
      (for [blog-post (get-blog-posts (:app/db context))]
        (blog-post-card blog-post))]]]

   ;; Tag filter script
   [:script "
     document.addEventListener('DOMContentLoaded', function() {
       const selectedTags = new Set();
       const tagButtons = document.querySelectorAll('#tag-filter button');
       const posts = document.querySelectorAll('.post');

       tagButtons.forEach(function(button) {
         button.addEventListener('click', function() {
           const tag = this.getAttribute('data-tag');

           if (selectedTags.has(tag)) {
             selectedTags.delete(tag);
             this.classList.remove('bg-blue-500', 'text-white');
             this.classList.add('bg-gray-200', 'text-gray-700');
           } else {
             selectedTags.add(tag);
             this.classList.remove('bg-gray-200', 'text-gray-700');
             this.classList.add('bg-blue-500', 'text-white');
           }

           filterPosts();
         });
       });

       function filterPosts() {
         posts.forEach(function(post) {
           const postTags = post.getAttribute('data-tags')?.split(',') || [];
           const isVisible = Array.from(selectedTags).some(function(tag) {
             return postTags.includes(tag);
           });
           post.style.display = (isVisible || selectedTags.size === 0) ? 'block' : 'none';
         });
       }
     });
   "]))

(defn render-article [context page]
  (layout {}
          header
          (hiccup/unescape (body-to-html-string (:page/body page)))))

(defn render-blog-post [context page]
  (let [body-html-str (body-to-html-string (:page/body page))
        headings (extract-headings body-html-str)
        toc (build-toc headings)]
    (layout
     {:title (:page/title page)
      :description (:page/description page "")}
     ;; Progress bar element
     [:div {:id "progress-bar"}]
     [:div {:class "bg-primary-200 min-h-screen"}
      [:div {:class "container mx-auto px-4 py-8"}
       ;; Title section
       [:h1 {:class "text-5xl font-extrabold text-center mb-4"}
        (:page/title page)]

       ;; Tags
       [:div {:class "text-center mb-4"}
        (for [tag (:blog-post/tags page)]
          [:span {:class "inline-block bg-gray-200 text-gray-700 text-sm font-semibold mr-2 px-2.5 py-0.5 rounded"}
           (name tag)])]

       ;; Description
       [:p {:class "text-xl text-center mb-2 font-sans text-gray-600 max-w-2xl mx-auto"}
        (:page/description page "")]

       ;; Date
       [:p {:class "text-sm text-center mb-8 font-mono text-gray-500"}
        (:page/date page "")]

       ;; Three-column layout
       [:div {:class "flex flex-col lg:flex-row gap-8"}
        ;; Left column - Table of Contents
        [:div {:class "lg:w-1/5 lg:sticky lg:top-24 lg:self-start"}
         (when (seq toc)
           [:nav {:class "toc"}
            [:div {:class "pb-4 text-2xl font-bold text-gray-800"}
             "Table of Contents"]
            [:ul {:class "font-sans"}
             (for [heading toc]
               [:li (render-toc-item heading)])]])]]]

        ;; Center column - Content
      [:article {:class "prose lg:prose-xl lg:w-4/5 font-sans max-w-none"}
       (hiccup/unescape body-html-str)]

        ;; Right column - Backlinks placeholder
      [:div {:class "lg:w-1/5 lg:sticky lg:top-24 lg:self-start"}
       [:div {:class "pb-4 text-2xl font-bold text-gray-800"}
        "Backlinks"]]]

     ;; TOC highlight script
     [:script "
       function updateActiveHeading() {
         const headings = document.querySelectorAll('h2, h3, h4, h5, h6');
         const tocLinks = document.querySelectorAll('.toc a');

         let currentActiveIndex = 0;
         const isAtBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 100;

         if (isAtBottom) {
           currentActiveIndex = headings.length - 1;
         } else {
           headings.forEach(function(heading, index) {
             const rect = heading.getBoundingClientRect();
             if (rect.top <= 100) {
               currentActiveIndex = index;
             }
           });
         }

         tocLinks.forEach(function(link, index) {
           if (index === currentActiveIndex) {
             link.classList.remove('text-gray-600', 'hover:text-gray-800', 'text-gray-500', 'hover:text-gray-700');
             link.classList.add('bg-blue-100', 'text-blue-800', 'font-bold', 'border-l-4', 'border-blue-500', 'pl-2');
           } else {
             link.classList.remove('bg-blue-100', 'text-blue-800', 'font-bold', 'border-l-4', 'border-blue-500', 'pl-2');
             link.classList.add('text-gray-600', 'hover:text-gray-800');
           }
         });
       }

       document.addEventListener('scroll', updateActiveHeading);
       window.addEventListener('resize', updateActiveHeading);
       updateActiveHeading();
     "])))

(defn render-page [context page]
  (case (:page/kind page)
    :page.kind/frontpage (render-frontpage context page)
    :page.kind/blog-post (render-blog-post context page)
    :page.kind/article (render-article context page)))

(def config
  {:site/title "The Powerblog"
   :datomic/schema-file "resources/schema.edn"
   :powerpack/port 8000
   :powerpack/log-level :debug
   :powerpack/render-page #'render-page
   :powerpack/create-ingest-tx #'create-tx
   :optimus/assets [{:public-dir "public"
                     :paths ["/favicon.svg" "/css/styles.css"]}]
   :optimus/options {:minify-js-assets? false
                     :minify-css-assets? false}})
