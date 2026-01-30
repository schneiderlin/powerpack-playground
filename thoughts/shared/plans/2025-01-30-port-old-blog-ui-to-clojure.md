# Port Old Blog UI to Clojure Blog Implementation Plan

## Overview

Port the visual design and UI components from the old Astro-based blog (`./linzihao`) to the new Clojure-based Powerpack blog. This includes adding Tailwind CSS styling via CDN, implementing all major UI components (Navbar, Footer, blog listing, blog post detail with TOC and progress bar), and migrating all content from the old blog to the new Clojure format.

## Current State Analysis

**Old Blog (Astro + React):**
- Beautiful UI with Tailwind CSS + shadcn/ui
- 37 blog posts in `src/content/blog/` (split into `en/` and `zh/` folders)
- YAML frontmatter: `title`, `author`, `tags`, `date`, `cover`, `description`, `ogImage`, `lang`
- Key components: Navbar (responsive), Footer (social links), BlogPostCard (masonry grid), TableOfContents (sticky with active highlighting), ReadProgressBar
- JavaScript features: tag filtering, TOC active heading, mobile menu, progress bar

**New Blog (Clojure + Powerpack):**
- Powerpack framework with Datomic database
- 11 existing blog posts in `content/blog-posts/`
- EDN frontmatter: `:page/title`, `:blog-post/tags`, `:blog-post/author`, `:page/body`
- Hiccup for HTML generation
- **Currently has NO CSS** - just raw HTML with minimal layout

### Key Discoveries:
- **File locations:**
  - Old blog posts: `linzihao/src/content/blog/en/*.md` and `linzihao/src/content/blog/zh/*.md`
  - New blog posts: `content/blog-posts/*.md`
  - New blog rendering: `src/powerblog/core.clj`
- **Frontmatter format difference:** Old uses YAML, new uses EDN (Clojure data structures)
- **Static assets:** Old blog has `public/favicon.svg`, `public/og.jpeg` - need to copy to new blog
- **Schema reference:** Old blog schema in `linzihao/src/content/config.ts`

## Desired End State

A fully styled Clojure blog that matches the visual appearance of the old Astro blog, with:
1. Tailwind CSS loaded via CDN in all pages
2. Responsive Navbar with logo and mobile hamburger menu
3. Footer with social media links
4. Homepage with masonry-grid blog post listing and tag filtering
5. Blog post detail page with:
   - Title, tags, description, date header
   - Three-column layout (TOC | content | backlinks placeholder)
   - Read progress bar at top
   - Sticky Table of Contents with active heading highlighting
6. All 37 old blog posts migrated to new Clojure format
7. Proper static assets (favicon, images)

## What We're NOT Doing

- **Dark mode** - Will be added later with specific user guidance
- **Comments system** - Skipping Firebase/Clerk integration from old blog
- **Authentication** - Skipping Clerk login integration
- **Command palette** (Cmd+Q search) - Skipping for now
- **Album/photo sections** - Focusing only on blog functionality
- **Local Tailwind build** - User will set this up later themselves

## Implementation Approach

**Phased approach:** Start with foundation (CSS + base layout), then add components progressively, then migrate content. Each phase is independently testable.

Use Tailwind CDN for simplicity (user will migrate to local build later). Convert Astro component patterns to Hiccup syntax. Extract CSS classes from old blog components for use in Clojure.

## Phase 1: Foundation - Tailwind CSS + Base Layout

### Overview
Add Tailwind CSS via CDN and create the base HTML layout structure with proper meta tags and viewport settings.

### Changes Required:

#### 1. Add Tailwind CDN to layout
**File:** `src/powerblog/core.clj`
**Changes:** Modify the `layout` function to include Tailwind CDN script and meta tags

```clojure
(defn layout [{:keys [title description lang]} & content]
  [:html {:lang (or lang "en")}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:meta {:name "description" :content (or description "Blog")}]
    (when title [:title title])
    [:link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"}]
    ;; Tailwind CSS CDN
    [:script {:src "https://cdn.tailwindcss.com"}]
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
    "]]
   [:body
    content]])
```

#### 2. Add body background style
**File:** `src/powerblog/core.clj`
**Changes:** Update `layout` function body to add background color

```clojure
[:body {:class "overflow-x-hidden" :style "background-color: #f0f0f0;"}
 content]
```

### Success Criteria:

#### Automated Verification:
- [ ] Blog compiles: `/clojure-eval (require 'powerblog.core :reload)`
- [ ] No errors when starting dev server
- [ ] HTML output contains Tailwind CDN script tag

#### Manual Verification:
- [ ] Visit homepage and verify Tailwind classes are working (inspect element in browser)
- [ ] Check that page has gray background (#f0f0f0)
- [ ] Verify viewport meta tag exists for responsive design

---

## Phase 2: Static Assets Migration

### Overview
Copy static assets (favicon, images) from old blog to new blog's public directory.

### Changes Required:

#### 1. Create public directory structure
**Action:** Create `public/` directory in root if it doesn't exist

#### 2. Copy assets
**Files to copy:**
- `linzihao/public/favicon.svg` → `public/favicon.svg`
- `linzihao/public/og.jpeg` → `public/og.jpeg`
- Any other needed images

### Success Criteria:

#### Automated Verification:
- [ ] `public/favicon.svg` exists
- [ ] `public/og.jpeg` exists

#### Manual Verification:
- [ ] Visit homepage and see favicon in browser tab
- [ ] Favicon displays correctly in browser

---

## Phase 3: Navbar Component

### Overview
Implement responsive Navbar component with logo, navigation links, and mobile hamburger menu (matches old blog's `Navbar.astro`).

### Changes Required:

#### 1. Create navbar component
**File:** `src/powerblog/components.clj` (new file)
**Changes:** Create navbar component with responsive menu

```clojure
(ns powerblog.components)

(def navbar
  [:nav {:class "bg-primary-400"}
   [:div {:class "flex flex-wrap items-center justify-between mx-auto py-4 px-8"}
    ;; Logo section
    [:a {:href "/" :class "flex items-center space-x-3"}
     [:img {:src "/favicon.svg" :class "h-8" :alt "LinZiHao's Digital Garden"}]
     [:span {:class "self-center text-2xl font-semibold whitespace-nowrap"}
      "LinZiHao's Digital Garden"]]

    ;; Mobile menu button
    [:button {:type "button"
              :class "inline-flex items-center p-2 w-10 h-10 justify-center text-sm rounded-lg md:hidden hover:bg-primary-100"
              :aria-controls "navbar-default"
              :aria-expanded "false"
              :id "mobile-menu-button"}
     [:span {:class "sr-only"} "打开菜单"]
     [:svg {:class "w-5 h-5" :aria-hidden "true" :xmlns "http://www.w3.org/2000/svg"
            :fill "none" :viewBox "0 0 17 14"}
      [:path {:stroke "currentColor" :stroke-linecap "round" :stroke-linejoin="round"
              :stroke-width "2" :d "M1 1h15M1 7h15M1 13h15"}]]]

    ;; Navigation links
    [:div {:class "w-full md:block md:w-auto hidden" :id "navbar-default"}
     [:ul {:class "flex flex-col p-4 md:p-0 mt-4 border md:flex-row md:space-x-8 md:mt-0 md:border-0 items-center"}
      [:li
       [:a {:href "/" :class "block py-2 px-3 text-gray-900 rounded hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-blue-700 md:p-0"}
        "Home"]]
      [:li
       [:a {:href "/about" :class "block py-2 px-3 text-gray-900 rounded hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-blue-700 md:p-0"}
        "About"]]]]]])

(def navbar-script
  "document.getElementById('mobile-menu-button').addEventListener('click', function() {
    var menu = document.getElementById('navbar-default');
    menu.classList.toggle('hidden');
  });")
```

#### 2. Update layout to include navbar
**File:** `src/powerblog/core.clj`
**Changes:** Add navbar to all pages and include mobile menu script

```clojure
(require '[powerblog.components :as components])

(defn layout [{:keys [title description lang]} & content]
  [:html {:lang (or lang "en")}
   [:head ...]
   [:body {:class "overflow-x-hidden" :style "background-color: #f0f0f0;"}
    components/navbar
    content
    [:script components/navbar-script]]])
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`
- [ ] No errors on page load

#### Manual Verification:
- [ ] Navbar appears at top of page
- [ ] Logo links to home page
- [ ] On desktop: horizontal menu visible
- [ ] On mobile: hamburger menu appears and toggles navigation links when clicked
- [ ] Hover effects work on navigation links

---

## Phase 4: Footer Component

### Overview
Implement Footer component with social media links (matches old blog's `Footer.astro`).

### Changes Required:

#### 1. Add footer component
**File:** `src/powerblog/components.clj`
**Changes:** Add footer with social links

```clojure
(def footer
  [:footer {:class "w-full bg-gray-100 py-8"}
   [:div {:class "container mx-auto px-4"}
    [:div {:class "flex flex-col items-center justify-center space-y-4"}
     ;; Social links
     [:nav {:class "flex flex-wrap justify-center gap-4"}
      [:a {:href "https://www.linkedin.com/in/zihao-lin-8b8067326/"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "LinkedIn"]
       "LinkedIn"]
      [:a {:href "https://x.com/fTJSaF2VnI11762"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "X (Twitter)"]
       "X"]
      [:a {:href "https://space.bilibili.com/375039815"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "Bilibili"]
       "Bilibili"]
      [:a {:href "https://www.zhihu.com/people/lin-zi-hao-8-89"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "Zhihu"]
       "Zhihu"]
      [:a {:href "https://github.com/schneiderlin"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "GitHub"]
       "GitHub"]]
     ;; Copyright
     [:p {:class "text-sm text-gray-500"}
      (str "© " (.getFullYear (java.time.LocalDate/now)) " LinZiHao's Digital Garden. All rights reserved.")]]]])
```

#### 2. Add footer to layout
**File:** `src/powerblog/core.clj`
**Changes:** Include footer before closing body tag

```clojure
(defn layout [{:keys [title description lang]} & content]
  [:html ...
   [:body ...
    components/navbar
    content
    components/footer
    [:script components/navbar-script]]])
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`

#### Manual Verification:
- [ ] Footer appears at bottom of page
- [ ] All social links are present
- [ ] Links open in new tab
- [ ] Hover effect changes link color

---

## Phase 5: Homepage - Blog Post Listing

### Overview
Implement masonry-grid blog post listing with tag filtering (matches old blog's `index.astro` and `BlogPostCard.astro`).

### Changes Required:

#### 1. Update frontpage rendering
**File:** `src/powerblog/core.clj`
**Changes:** Rewrite `render-frontpage` with styled masonry grid

```clojure
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

(defn render-frontpage [context page]
  (layout
   {:title "LinZiHao's Digital Garden"
    :description "LinZiHao's Digital Garden"}
   (md/render-html (:page/body page))
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
        (blog-post-card blog-post))]]]]))

;; Add helper function to get all unique tags
(defn get-all-tags [db]
  (->> (d/q '[:find [?tag ...]
              :where
              [_ :blog-post/tags ?tag]]
            db)
       set))

;; Add tag filtering script
(def tag-filter-script
  "document.addEventListener('DOMContentLoaded', function() {
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
  });")
```

#### 2. Update layout to include tag filter script
**File:** `src/powerblog/core.clj`
**Changes:** Add tag filter script to layout

```clojure
[:body ...
 components/navbar
 content
 components/footer
 [:script components/navbar-script]
 [:script tag-filter-script]]
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`

#### Manual Verification:
- [ ] Homepage displays hero section with three headings
- [ ] Blog posts display in masonry grid (3 columns on large screens)
- [ ] Each post card shows: title, tags, description, date
- [ ] Clicking a tag button toggles its visual state (gray → blue)
- [ ] Filtering works: only posts with selected tags are visible
- [ ] Clicking multiple tags shows posts matching any tag
- [ ] Clicking all tags again shows all posts

---

## Phase 6: Blog Post Detail Page - Layout

### Overview
Implement blog post detail page with header section (title, tags, description, date) and three-column layout structure.

### Changes Required:

#### 1. Update blog post rendering
**File:** `src/powerblog/core.clj`
**Changes:** Rewrite `render-blog-post` with styled layout

```clojure
(defn render-blog-post [context page]
  (layout
   {:title (:page/title page)
    :description (:page/description page "")}
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
      ;; Left column - Table of Contents placeholder
      [:div {:class "lg:w-1/5 lg:sticky lg:top-24 lg:self-start"}
       [:div {:class "pb-4 text-2xl font-bold text-gray-800"}
        "Table of Contents"]]

      ;; Center column - Content
      [:article {:class "prose lg:prose-xl lg:w-4/5 font-sans max-w-none"}
       (md/render-html (:page/body page))]

      ;; Right column - Backlinks placeholder
      [:div {:class "lg:w-1/5 lg:sticky lg:top-24 lg:self-start"}
       [:div {:class "pb-4 text-2xl font-bold text-gray-800"}
        "Backlinks"]]]]]))
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`

#### Manual Verification:
- [ ] Blog post page displays with proper styling
- [ ] Title is centered and large (5xl, font-extrabold)
- [ ] Tags display as gray badges
- [ ] Description and date are centered
- [ ] On large screens: three-column layout visible (TOC | content | backlinks)
- [ ] On small screens: columns stack vertically

---

## Phase 7: Read Progress Bar

### Overview
Add read progress bar at top of blog post pages (matches old blog's `ReadProgressBar.astro`).

### Changes Required:

#### 1. Add progress bar HTML and script
**File:** `src/powerblog/core.clj`
**Changes:** Add progress bar element to blog post layout

```clojure
(defn render-blog-post [context page]
  (layout
   {:title (:page/title page)
    :description (:page/description page "")}
   ;; Progress bar (before main content)
   [:div {:id "progress-bar"}]
   [:div {:class "bg-primary-200 min-h-screen"}
    ...]))

;; Add progress bar script
(def progress-bar-script
  "function updateProgressBar() {
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
  updateProgressBar();")
```

#### 2. Add progress bar styles to Tailwind config
**File:** `src/powerblog/core.clj` in layout function
**Changes:** Add custom styles to Tailwind config

```clojure
;; In the Tailwind config script:
[:script "
  tailwind.config = {
    ...
  }
"]
;; Add style element for progress bar
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
"]
```

#### 3. Include progress bar script
**File:** `src/powerblog/core.clj`
**Changes:** Add to layout body scripts

```clojure
[:body ...
 [:script components/navbar-script]
 [:script tag-filter-script]
 [:script progress-bar-script]]
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`

#### Manual Verification:
- [ ] Progress bar appears at top of blog post page
- [ ] Scrolling down increases progress bar width
- [ ] Progress bar reaches full width at bottom of page
- [ ] Progress bar stays fixed at top (doesn't scroll with content)
- [ ] Progress bar color is #502020 (dark red/brown)

---

## Phase 8: Table of Contents with Active Highlighting

### Overview
Implement Table of Contents component that highlights the currently visible heading (matches old blog's `TableOfContents.astro`).

### Changes Required:

#### 1. Extract headings from markdown
**File:** `src/powerblog/core.clj`
**Changes:** Add function to parse headings from rendered HTML

```clojure
(require '[clojure.string :as str])

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
```

#### 2. Render TOC in blog post page
**File:** `src/powerblog/core.clj`
**Changes:** Update `render-blog-post` to include TOC

```clojure
(defn render-toc-item [heading]
  (let [base-class (if (= (:depth heading) 2)
                     "block py-2 px-3 text-gray-600 hover:text-gray-800"
                     "block py-1 pl-6 text-sm text-gray-500 hover:text-gray-700")]
    [:a {:href (str "#" (:slug heading))
         :class base-class
         :data-heading (:slug heading)}
     (:text heading)]

    (when (seq (:subheadings heading))
      [:div
       (for [sub (:subheadings heading)]
         (render-toc-item sub))]))

(defn render-blog-post [context page]
  (let [body-html (md/render-html (:page/body page))
        headings (extract-headings body-html)
        toc (build-toc headings)]
    (layout
     {:title (:page/title page)
      :description (:page/description page "")}
     [:div {:id "progress-bar"}]
     [:div {:class "bg-primary-200 min-h-screen"}
      [:div {:class "container mx-auto px-4 py-8"}
       ;; Title section
       ...
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
               [:li (render-toc-item heading)])]])]

        ;; Center column - Content
        [:article {:class "prose lg:prose-xl lg:w-4/5 font-sans max-w-none"}
         body-html]

        ;; Right column - Backlinks placeholder
        ...]]]])))
```

#### 3. Add TOC active highlighting script
**File:** `src/powerblog/core.clj`
**Changes:** Add script for active heading highlighting

```clojure
(def toc-highlight-script
  "function updateActiveHeading() {
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
  updateActiveHeading();")
```

#### 4. Include TOC script
**File:** `src/powerblog/core.clj`
**Changes:** Add to layout body scripts

```clojure
[:body ...
 [:script components/navbar-script]
 [:script tag-filter-script]
 [:script progress-bar-script]
 [:script toc-highlight-script]]
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `/clojure-eval (require 'powerblog.core :reload)`
- [ ] Headings are extracted correctly from markdown

#### Manual Verification:
- [ ] Table of Contents displays with all h2, h3, h4 headings
- [ ] Clicking a TOC link scrolls to that heading
- [ ] When scrolling, the active heading is highlighted in TOC (blue background, bold, left border)
- [ ] At bottom of page, last heading is highlighted
- [ ] TOC is sticky on large screens (stays visible while scrolling)

---

## Phase 9: Content Migration

### Overview
Migrate all 37 blog posts from old Astro blog to new Clojure blog format, converting YAML frontmatter to EDN format.

### Changes Required:

#### 1. Create migration script
**File:** `scripts/migrate-content.clj` (new file)
**Changes:** Create script to convert old blog posts

```clojure
#!/usr/bin/env bb
(ns migrate-content
  (:require [clojure.string :as str]
            [clojure.edn :as edn]))

(defn parse-yaml-frontmatter [content]
  (let [frontmatter-regex #"^---\n(.*?)\n---"
        frontmatter-match (re-find frontmatter-regex content)]
    (if frontmatter-match
      (let [yaml-content (second frontmatter-match)
            body-content (str/replace content frontmatter-regex "")
            ;; Simple YAML to EDN conversion for our schema
            pairs (re-seq #"(\w+):\s*(.*(?:\n\s+\+.*)*)" yaml-content)]
        {:frontmatter (into {} (map (fn [[k v]]
                                      [(keyword "page" (str/lower-case k))
                                       (str/trim v)])
                                    pairs))
         :body body-content})
      {:frontmatter {}
       :body content})))

(defn convert-frontmatter-to-edn [frontmatter]
  (let [tags (when-let [tags-str (:tags frontmatter)]
                (mapv keyword (str/split tags-str #"[,\s]+")))]
    {:page/title (or (:title frontmatter) "Untitled")
     :page/description (or (:description frontmatter) "")
     :page/date (or (:date frontmatter) "")
     :blog-post/tags tags
     :blog-post/author {:person/id :jan}}))

(defn migrate-file [input-path output-path]
  (let [content (slurp input-path)
        {:keys [frontmatter body]} (parse-yaml-frontmatter content)
        edn-frontmatter (convert-frontmatter-to-edn frontmatter)]
    (spit output-path
          (str/join "\n"
                    [(str ":page/title " (pr-str (:page/title edn-frontmatter)))
                     (str ":page/description " (pr-str (:page/description edn-frontmatter)))
                     (str ":page/date " (pr-str (:page/date edn-frontmatter)))
                     (str ":blog-post/tags " (pr-str (:blog-post/tags edn-frontmatter)))
                     ":blog-post/author {:person/id :jan}"
                     ":page/body"
                     ""])))
    (spit output-path body :append true)))

;; Main migration
(defn -main []
  (println "Migrating blog posts...")
  ;; List all files from linzihao/src/content/blog/
  ;; Convert and save to content/blog-posts/
  (println "Migration complete!"))
```

#### 2. Manual migration approach
Since YAML parsing can be complex, migrate posts manually or semi-automatically:

**Example conversion:**

Old format (`linzihao/src/content/blog/en/writing_environment.md`):
```markdown
---
title: "Strategies for Effortless and Impactful Writing"
author: linzihao
date: "2024-09-06"
description: "Discover practical techniques..."
lang: "en"
tags: ["blog", "writing"]
---
```

New format (`content/blog-posts/writing-environment.md`):
```clojure
:page/title "Strategies for Effortless and Impactful Writing"
:page/description "Discover practical techniques..."
:page/date "2024-09-06"
:blog-post/tags [:blog :writing]
:blog-post/author {:person/id :jan}
:page/body
```

### Content Files to Migrate:

**English posts (from `linzihao/src/content/blog/en/`):**
- `how_to_do_shadowing.mdx` → `how-to-do-shadowing.md`
- `decompose_task.md` → `decompose-task.md`
- `build_my_workout_app.mdx` → `build-my-workout-app.md`
- `blog_standard.md` → `blog-standard.md`
- `writing_environment.md` → `writing-environment.md`
- `work_fulfillment.md` → `work-fulfillment.md`
- `interactive.mdx` → `interactive.md`
- `java_mess.md` → `java-mess.md`
- `links.md` → `links.md`

**Chinese posts (from `linzihao/src/content/blog/zh/`):**
- `build-language-in-clojure.md`
- `database_storage.md` → `database-storage.md`
- `function_value_2.md` → `function-value-2.md`
- `hardware.md`
- `network_layer.md` → `network-layer.md`
- `out_of_core.md` → `out-of-core.md`
- `applicative.md`
- `distribute-tracing-conceps.md` → `distribute-tracing-concepts.md`
- `MessageChannel.md` → `message-channel.md`
- `mqBrokerException_14.md` → `mq-broker-exception.md`
- `mutex-in-akka-1-1.md` → `mutex-in-akka.md`
- `out-of-core-sorting-rust.md`
- `postmortem_bahasa_project1.md` → `postmortem-bahasa-project.md`
- `rocketmq_delay_msg.md` → `rocketmq-delay-message.md`
- `third_time_work_postmortem.md` → `third-time-work-postmortem.md`
- `functionk.md`
- `kleisli.md`
- `lens-iso.md` → `lens-iso.md`
- `lens.md`
- `rocketmq_disk_full.md` → `rocketmq-disk-full.md`
- `string-search-1.md` → `string-search.md`
- `profunctor.md`
- `ft-vm.md`
- `gfs.md`
- `map-reduce.md`
- `raft.md`
- `information_source.md` → `information-source.md`
- `contravariant_intuition.md` → `contravariant-intuition.md`
- `astro_blog.mdx` → `astro-blog.md`
- `at-most-once.md`

### Success Criteria:

#### Automated Verification:
- [ ] All migrated files exist in `content/blog-posts/`
- [ ] Each file has valid EDN frontmatter
- [ ] Content can be loaded by Powerpack

#### Manual Verification:
- [ ] Homepage lists all migrated posts
- [ ] Clicking a post link displays the post correctly
- [ ] Title, tags, date display correctly
- [ ] Post content renders markdown correctly
- [ ] Chinese characters display properly

---

## Phase 10: Schema Updates (if needed)

### Overview
Update database schema to support additional frontmatter fields if needed.

### Changes Required:

#### 1. Review and update schema
**File:** `resources/schema.edn`
**Changes:** Add any missing attributes

Current schema:
```clojure
[{:db/ident :blog-post/author
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one}
 {:db/ident :blog-post/tags
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/many}
 {:db/ident :person/id
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/one
  :db/unique :db.unique/identity}
 {:db/ident :person/full-name
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one}]
```

Check if we need to add:
- `:page/description` - for post descriptions
- `:page/date` - for post dates

### Success Criteria:

#### Automated Verification:
- [ ] Schema file updated if needed
- [ ] Database migration completes successfully

---

## Testing Strategy

### Unit Tests:
- Test `extract-headings` function with various HTML inputs
- Test `build-toc` function with heading hierarchies
- Test `get-all-tags` function returns correct tags

### Integration Tests:
- Test full page rendering for frontpage
- Test full page rendering for blog post
- Test tag filtering interaction

### Manual Testing Steps:
1. **Visual Regression:** Take screenshots of old blog and compare with new blog side-by-side
2. **Responsive Design:** Test on mobile, tablet, desktop viewports
3. **Interactivity:** Click all buttons, links, test filtering
4. **Content Verification:** Read through migrated posts for formatting issues
5. **Performance:** Check page load times with Tailwind CDN

## Performance Considerations

- **Tailwind CDN:** Initial page load may be slower until user sets up local Tailwind
- **JavaScript:** Keep scripts minimal and defer loading where possible
- **Images:** Old blog uses Astro Image optimization - new blog will serve images directly (may need optimization later)

## Migration Notes

1. **Content folder structure:** Old blog separates posts by language (en/ and zh/ folders) - new blog keeps all posts in single `blog-posts/` folder
2. **URL structure:** Old blog uses `/blog/{slug}` - new blog currently uses direct paths - may need adjustment
3. **Author references:** Old blog has author as string, new blog uses entity reference - ensure `{:person/id :jan}` exists
4. **Date format:** Old blog uses ISO date strings - keep same format for migration
5. **Slug conversion:** Old blog filenames use snake_case - convert to kebab-case for consistency

## References

- Old blog styling: `linzihao/src/styles/globals.css`
- Old blog components: `linzihao/src/components/`
- Old blog config: `linzihao/src/content/config.ts`
- Old blog tailwind config: `linzihao/tailwind.config.mjs`
- New blog core: `src/powerblog/core.clj`
- New blog schema: `resources/schema.edn`
- New blog content: `content/blog-posts/`
