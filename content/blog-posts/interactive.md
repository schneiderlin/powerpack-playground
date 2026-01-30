:page/title "Exploring Interactivity in Astro"
:page/description "Discovering the interactive capabilities of Astro-powered websites using MDX"
:page/date "2024-09-12"
:blog-post/tags [:astro]
:blog-post/author {:person/id :jan}
:page/body

import { Alert, AlertTitle, AlertDescription } from '@components/ui/alert';
import Counter from "@/components/Counter";
import Reagent from "@/components/Reagent.astro";

## Interactivity

How interactive can we make this website? Let's explore the possibilities!

I've built this blog using Astro, as detailed in my [blog setup guide](/blog/zh/astro_blog). 
The content is written in Markdown or MDX, which Astro then renders. MDX allows me to incorporate HTML and React components directly into my content, 
opening up a world of interactive possibilities.

### HTML

With MDX, I can seamlessly integrate HTML into my content. Here's a simple example:

```html
<div>
    <h1>Hello there!</h1>
    <h2>Welcome to my interactive playground</h2>
    <a id="example_anchor"></a>
    <h3>Let's explore together</h3>
</div>
```

<div>
    <h1>Hello there!</h1>
    <h2>Welcome to my interactive playground</h2>
    <a id="example_anchor"></a>
    <h3>Let's explore together</h3>
</div>

### react component
or I can use a react component
```tsx
import { Alert, AlertTitle, AlertDescription } from '@components/ui/alert';

<Alert variant="warning">
    <AlertTitle>Alert</AlertTitle>
    <AlertDescription>
        Alert description
    </AlertDescription>
</Alert>
```

<Alert variant="warning">
    <AlertTitle>Alert</AlertTitle>
    <AlertDescription>
        Alert description
    </AlertDescription>
</Alert>

but these are just content, how about a counter in react?
```tsx
import Counter from "@/components/Counter"

<Counter client:visible />
```

<Counter client:visible />

I need to add `client:visible` to make this component interactive. because astro default to strip all javascript for performance reason.

### link
If I want to quote some pharagraph from my other post, or this post. 
I can write a anchor tag with some id in the target location. for example, I write an anchor in the `<h3>` element before.
```html
<a id="example_a"></a>
<h3>hello</h3>
```

now I can link to it.
```html
<a href="#example_a">example a</a>
```

<a href="#example_a">example a</a>

### backlink
this is not implemented yet, but I want to know what other pages link to this paragraph.
I am thinking of using some tool to read and parse all the markdown file, and output a index file.
then I can see all the links between pages.

haven't figured out how to user interaction would be like, maybe when the user highlight some text, then show a popup window with links to
this paragraph?

### clojure 
I want whatever in the clojure block be render.
these clojure code 

<Reagent>
    [:div "hello"]
</Reagent>

Currently, this is implemented by writing an Astro component. 
In that component, I import Scittle (for executing ClojureScript in the browser) and create some HTML elements: a textarea and buttons.
The ClojureScript code retrieves the user input from the textarea and renders it using Reagent (a React wrapper for ClojureScript).

```clojure
(require '[reagent.core :as r]
         '[reagent.dom :as rdom])

(def app (.getElementById js/document "app"))
(def code (.getElementById js/document "clojure_code"))
(def button (.getElementById js/document "re_render_button"))

(.addEventListener button "click"
                   (fn []
                     (let [code (.-value code)
                           form (read-string code)
                           _ (println form)] 
                       (rdom/render form (.getElementById js/document "app")))))
```

there is a problem in the current implementation, I can't have two clojure component in one page. because the element ids are hardcoded.