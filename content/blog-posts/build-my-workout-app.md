:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: Build My Workout App
author: linzihao
tags: ["mobile app", "react", "react native"]
date: "2024-09-04"
lang: "en"
cover: "workout_app/3.jpg"
description: "Why and How I built my workout App in 3 days"
---

import { Image } from 'astro:assets';
import i1 from '@/assets/workout_app/1.jpg';
import i2 from '@/assets/workout_app/2.jpg';
import i3 from '@/assets/workout_app/3.jpg';

## The Result
I am very happy with the result, it is a simple and useful app.
I didn't upload it to the app store, it is just for my personal use, so I built an apk locally and installed it on my phone.

Here are some screenshots:
<div class="flex">
    <Image src={i1} alt="i1" width="300" height="300" />
    <Image src={i2} alt="i2" width="300" height="300" />
    <Image src={i3} alt="i3" width="300" height="300" />
</div>


Developing a mobile app using react native is very efficient. If I were to build the same app again with RN experience, I could probably finish it in 3 hours.

## Why I built this app
I was using Lianlian to track my workouts before, but recently I needed to export the data, and wanted to do some custom analysis and visualization.
However, Lianlian doesn't support exporting data, even though I am a lifetime paid user, so I decided to build my own workout app.

## The Goal
1. Have a pool of exercises to choose from
2. Be able to record date, exercise name, set, repetition, and weight
3. And obviously, export the data

The UI should be simple, without too many features, just basic functions.

## The tech stack
I chose react native, because I was already familiar with react. Also, RN is a popular choice, so there's more training data for AI coding assistants.
For the backend, I decided to just use a local database to store the data. There's no need for a backend server; it can fully work offline.
I used expo for development and building the app. It is easy to set up and use; you just start a dev server and scan the QR code with your phone. I didn't set up any simulators.
I used tanstack query for data fetching from the database. It can manage async operations and caching.

The exercise data and images are from wger.de; it is free and open source.
I grabbed the data from their API, and put it in some json files.

## Development Process
It took a total of 3 days.

```
9526cc8 HEAD@{0}     2 hours ago commit: export db (export db)
0d8722a HEAD@{1}     2 hours ago commit: remove second tab (remove second tab)
fb8159f HEAD@{2}     3 hours ago commit: log add ex id field (log add ex id field)
901b5f9 HEAD@{3}     3 hours ago commit: detail ui (detail ui)
4db5717 HEAD@{4}     3 hours ago commit: refactor (refactor)
aa43e8f HEAD@{5}     4 hours ago commit: deselect (deselect)
5abe148 HEAD@{6}     4 hours ago commit: equipment fixture (equipment fixture)
960d1f0 HEAD@{7}     4 hours ago commit: filter by category (filter by category)
c495c6c HEAD@{8}     4 hours ago commit: use category json (use category json)
0763800 HEAD@{9}     4 hours ago commit: use json image (use json image)
611b5e5 HEAD@{10}    4 hours ago commit: fix image not shown (fix image not shown)
c0cdc27 HEAD@{11}    4 hours ago commit: use json data (use json data)
8d76d01 HEAD@{12}    14 hours ago commit: database CRUD (database CRUD)
6628127 HEAD@{13}    15 hours ago commit: android build (android build)
39d46d9 HEAD@{14}    31 hours ago commit: use store (use store)
a8d391f HEAD@{15}    2 days ago commit: some bug (some bug)
0cf289e HEAD@{16}    2 days ago commit: db (db)
7eb70d1 HEAD@{17}    2 days ago commit: setup sqlite (setup sqlite)
b503046 HEAD@{18}    2 days ago commit: rename (rename)
7f2199a HEAD@{19}    2 days ago reset: moving to 7f2199a (init)
7669b07 HEAD@{20}    2 days ago checkout: moving from 7f2199afaf19a24b9236f3924bc386781e97d531 to main (persistent basic)
7f2199a HEAD@{21}    2 days ago checkout: moving from main to 7f2199afaf19a24b9236f3924bc386781e97d531 (init)
7669b07 HEAD@{22}    2 days ago commit: persistent basic (persistent basic)
45c3073 HEAD@{23}    2 days ago commit: android build (android build)
7f2199a HEAD@{24}    2 days ago commit (initial): init (init)
```

The first day I built the UI, mainly using Cursor to generate the code. This was very fast even for someone with no experience with react native.
expo uses file-based routing, similar to nextjs.

The second day I experimented with many forms of data storage, and finally chose sqlite. 
firebase requires a network connection, and I tried to set up realm without success; some documentation was out of date.

I created a sqlite database file, and initialized the tables. I put the db file into the assets folder for expo to bundle it. Then I used expo-file-system to read and write to the db file.

The db file in the assets folder is the initial empty db, and it is read-only. When the app starts, it checks if the db file exists in the document folder. If not, it copies the db file from assets to the document folder, and uses the db in the document folder.

```typescript
const loadDatabase = async () => {
  const dbName = "workout.db"
  const dbAsset = require(`../assets/${dbName}`)
  const dbUri = Asset.fromModule(dbAsset).uri;
  const dbFilePath = `${FileSystem.documentDirectory}SQLite/${dbName}`
  
  const fileInfo = await FileSystem.getInfoAsync(dbFilePath)
  if (!fileInfo.exists) {
    await FileSystem.makeDirectoryAsync(`${FileSystem.documentDirectory}SQLite`,
       { intermediates: true })
    await FileSystem.downloadAsync(dbUri, dbFilePath)
  }
}
```

There is a logAPI.ts file, responsible for reading and writing log data to the db, and also getting exercise data from the json file I loaded from the wger.de API.
```typescript
import * as SQLite from 'expo-sqlite';
import { Category, Equipment, ExerciseBase } from './interfaces';

export const allData: ExerciseBase[] = require(`../assets/exercises_all.json`)
export const categories: Category[] = require(`../assets/category.json`)
export const equipments: Equipment[] = require(`../assets/equipment.json`)

export const getImageByExerciseId = (exerciseId: number) => {
    const exercise = allData.find((exercise) => exercise.id === exerciseId)
    return exercise?.images[0].image
}

export async function getLogs(db: SQLite.SQLiteDatabase, date: string) {
    const logs = await db.getAllAsync("select * from exercise_log where date = ?", [date])
    const logIds = logs.map((log: any) => log.id)
    const sets = await db.getAllAsync(`select * from exercise_set where exercise_log_id in (${logIds.join(",")})`)
    return logs.map((log: any) => {
        const logSets = sets
            .filter((set: any) => set.exercise_log_id === log.id)
            .map((set: any) => ({
                ...set,
                finish: Boolean(set.finish)
            }))
        const result = { ...log, sets: logSets }
        return result
    })
}

......
```

And I used tanstack query to make a custom hook, exposing some user-friendly functions to the UI.
```typescript
import { getLogs, createLog, createSet, deleteSet as deleteSet1, deleteLog, updateSet as updateSet1 } from "@/store/logAPI";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSQLiteContext } from "expo-sqlite";

export function useWorkout(date: string) {
    const queryClient = useQueryClient();
    const db = useSQLiteContext();
    const { data: exerciseLogs, isLoading } = useQuery({
        queryKey: ['exerciseLog', date],
        queryFn: () => {
            return getLogs(db, date)
        },
    });
    const { mutate: addExercise } = useMutation({
        mutationFn: ({date, name, exerciseId}: {date: string, name: string, exerciseId: number}) => createLog(db, date, name, exerciseId)
    });
    const {mutate: deleteExercise} = useMutation({
        mutationFn: (id: number) => deleteLog(db, id)
    });
    const { mutate: addSet } = useMutation({
        mutationFn: ({exerciseLogId, weight, reps}: 
            {exerciseLogId: number, weight: number, reps: number}) =>
                 createSet(db, exerciseLogId, weight, reps)
    });
    const { mutate: updateSet } = useMutation({
        mutationFn: ({id, weight, reps, finish}: {id: number, weight: number, reps: number, finish: boolean}) => {
            console.log("updateSet", id, weight, reps, finish)
            return updateSet1(db, id, weight, reps, finish)
        }
    });
    const { mutate: deleteSet } = useMutation({
        mutationFn: (id: number) => deleteSet1(db, id)
    });
    return {
        exerciseLogs,
        isLoading,
        addExercise,
        deleteExercise,
        addSet,
        updateSet,
        deleteSet,
    }
}
```

On the third day, I added an export feature. I just needed to export the whole db as a file.
I used expo-sharing, so the user can choose to share the db file with other apps, or save it to the file system.

```typescript
export const exportDatabase = async () => {
  const dbName = "workout.db";
  const dbFilePath = `${FileSystem.documentDirectory}SQLite/${dbName}`;
  
  if (await FileSystem.getInfoAsync(dbFilePath).then(res => res.exists)) {
    // Check if sharing is available on the device
    if (await Sharing.isAvailableAsync()) {
      try {
        await Sharing.shareAsync(dbFilePath, { UTI: '.db', mimeType: 'application/x-sqlite3' });
      } catch (error) {
        console.error('Error sharing database:', error);
      }
    } else {
      console.log('Sharing is not available on this device');
    }
  } else {
    console.log('Database file does not exist');
  }
};
```